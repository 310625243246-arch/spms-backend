package com.spms.service;

import com.spms.dto.*;
import com.spms.entity.*;
import com.spms.exception.BadRequestException;
import com.spms.exception.ResourceNotFoundException;
import com.spms.repository.BookingRepository;
import com.spms.repository.OfficerRepository;
import com.spms.repository.PaymentRepository;
import com.spms.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficerService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final List<BookingStatus> WAITING_STATUSES =
            List.of(BookingStatus.BOOKED, BookingStatus.CHECKED_IN, BookingStatus.IN_QUEUE,
                    BookingStatus.GRADING, BookingStatus.WEIGHING, BookingStatus.PAYMENT_PENDING);

    private final OfficerRepository officerRepository;
    private final BookingRepository bookingRepository;
    private final TokenRepository tokenRepository;
    private final PaymentRepository paymentRepository;
    private final QueueService queueService;
    private final NotificationService notificationService;

    public Officer getOfficerByUserId(Long userId) {
        return officerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer profile not found."));
    }

    public OfficerDashboardResponse getDashboard(Long userId) {
        Officer officer = getOfficerByUserId(userId);
        if (officer.getProcurementCenter() == null) {
            throw new BadRequestException("This officer is not assigned to a procurement center.");
        }
        Long centerId = officer.getProcurementCenter().getId();
        LocalDate today = LocalDate.now();

        long totalToday = bookingRepository.countByProcurementCenterIdAndDate(centerId, today);
        long completed = bookingRepository.countByProcurementCenterIdAndDateAndStatus(centerId, today, BookingStatus.COMPLETED);
        long waiting = bookingRepository.countByProcurementCenterIdAndDateAndStatusIn(centerId, today, WAITING_STATUSES);

        double quantityProcured = bookingRepository.findByProcurementCenterIdAndDateOrderByStartTimeAsc(centerId, today)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .mapToDouble(b -> b.getQuantityKg() != null ? b.getQuantityKg() : 0)
                .sum();

        double amountPaidToday = paymentRepository.findAllByOrderByPaymentDateDesc().stream()
                .filter(payment -> today.equals(payment.getPaymentDate()))
                .filter(payment -> "Completed".equalsIgnoreCase(payment.getStatus()))
                .filter(payment -> payment.getBooking() != null
                        && centerId.equals(payment.getBooking().getProcurementCenter().getId()))
                .mapToDouble(payment -> payment.getAmount().doubleValue())
                .sum();

        String currentToken = tokenRepository
                .findByBooking_ProcurementCenterIdAndBooking_DateOrderByQueuePositionAsc(centerId, today)
                .stream()
                .filter(t -> t.getStatus() == TokenStatus.CALLED || t.getStatus() == TokenStatus.IN_PROGRESS)
                .map(Token::getTokenNumber)
                .findFirst()
                .orElse("-");

        OfficerStatsDto stats = new OfficerStatsDto(totalToday, waiting, completed, currentToken, quantityProcured, amountPaidToday);

        List<QueueTableRowDto> queueRows = bookingRepository
                .findByProcurementCenterIdAndDateOrderByStartTimeAsc(centerId, today)
                .stream()
                .map(this::toQueueRow)
                .toList();

        return new OfficerDashboardResponse(officer.getProcurementCenter().getName(), today.toString(), stats, queueRows);
    }

    public List<QueueTableRowDto> getQueue(Long userId) {
        Officer officer = getOfficerByUserId(userId);
        Long centerId = officer.getProcurementCenter().getId();
        return bookingRepository.findByProcurementCenterIdAndDateOrderByStartTimeAsc(centerId, LocalDate.now())
                .stream()
                .map(this::toQueueRow)
                .toList();
    }

    @Transactional
    public QueueTableRowDto callNext(Long userId) {
        Officer officer = getOfficerByUserId(userId);
        Long centerId = officer.getProcurementCenter().getId();

        Token token = queueService.callNext(centerId, LocalDate.now())
                .orElseThrow(() -> new ResourceNotFoundException("No farmers waiting in the queue."));

        notificationService.notify(token.getBooking().getFarmer().getUser(),
                "You have been called at " + officer.getProcurementCenter().getName() + ". Please proceed to the counter.");

        return toQueueRow(token.getBooking());
    }

    @Transactional
    public QueueTableRowDto updateStatus(Long userId, String tokenNumber, QueueStatusUpdateRequest request) {
        Officer officer = getOfficerByUserId(userId);
        Token token = tokenRepository.findByTokenNumber(tokenNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found: " + tokenNumber));

        Booking booking = token.getBooking();
        if (!booking.getProcurementCenter().getId().equals(officer.getProcurementCenter().getId())) {
            throw new ResourceNotFoundException("Token not found: " + tokenNumber);
        }

        BookingStatus newStatus = BookingStatus.fromDisplay(request.status());
        booking.setStatus(newStatus);
        bookingRepository.save(booking);

        if (newStatus == BookingStatus.GRADING || newStatus == BookingStatus.WEIGHING) {
            token.setStatus(TokenStatus.IN_PROGRESS);
        } else if (newStatus == BookingStatus.COMPLETED) {
            token.setStatus(TokenStatus.COMPLETED);
            recordPayment(booking);
        } else if (newStatus == BookingStatus.REJECTED) {
            token.setStatus(TokenStatus.SKIPPED);
        }
        tokenRepository.save(token);

        notificationService.notify(booking.getFarmer().getUser(),
                "Your procurement status is now: " + newStatus.toDisplay() + ".");

        return toQueueRow(booking);
    }

    private void recordPayment(Booking booking) {
        BigDecimal ratePerKg = new BigDecimal("21");
        BigDecimal quantity = BigDecimal.valueOf(booking.getQuantityKg() != null ? booking.getQuantityKg() : 0);
        BigDecimal amount = quantity.multiply(ratePerKg);

        Payment payment = Payment.builder()
                .farmer(booking.getFarmer())
                .booking(booking)
                .amount(amount)
                .paymentDate(LocalDate.now())
                .status("Completed")
                .transactionReference("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();
        paymentRepository.save(payment);

        notificationService.notify(booking.getFarmer().getUser(),
                String.format("Payment of Rs.%.2f for your procurement has been processed.", amount));
    }

    private QueueTableRowDto toQueueRow(Booking b) {
        String tokenNumber = tokenRepository.findByBookingId(b.getId())
                .map(Token::getTokenNumber)
                .orElse("-");
        return new QueueTableRowDto(
                tokenNumber, b.getFarmer().getName(), b.getCrop().getName(),
                b.getStartTime().format(TIME_FORMAT), b.getStatus().toDisplay());
    }
}
