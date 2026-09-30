package com.spms.service;

import com.spms.dto.*;
import com.spms.entity.*;
import com.spms.exception.BadRequestException;
import com.spms.exception.ResourceNotFoundException;
import com.spms.repository.*;
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
public class FarmerService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final FarmerRepository farmerRepository;
    private final ProcurementCenterRepository procurementCenterRepository;
    private final CropRepository cropRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final QueueService queueService;
    private final NotificationService notificationService;

    public Farmer getFarmerByUserId(Long userId) {
        return farmerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found."));
    }

    public FarmerDashboardResponse getDashboard(Long userId) {
        Farmer farmer = getFarmerByUserId(userId);

        FarmerProfileDto profileDto = new FarmerProfileDto(
                farmer.getName(), farmer.getFarmerId(), farmer.getVillage(), farmer.getDistrict(), farmer.getPhone());

        List<Booking> bookings = bookingRepository.findByFarmerIdOrderByDateDescCreatedAtDesc(farmer.getId());

        Booking upcoming = bookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.COMPLETED && b.getStatus() != BookingStatus.REJECTED)
                .findFirst()
                .orElse(null);

        UpcomingBookingDto upcomingDto = null;
        QueueInfoDto queueInfoDto = null;

        if (upcoming != null) {
            upcomingDto = toUpcomingDto(upcoming);
            queueInfoDto = buildQueueInfo(upcoming);
        }

        List<ProcurementHistoryDto> history = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED || b.getStatus() == BookingStatus.REJECTED)
                .map(this::toHistoryDto)
                .toList();

        List<NotificationDto> notifications = notificationService.getForUser(userId);

        return new FarmerDashboardResponse(profileDto, upcomingDto, queueInfoDto, notifications, history);
    }

    public List<ProcurementCenterDto> listCenters() {
        return procurementCenterRepository.findAll().stream()
                .filter(ProcurementCenter::isActive)
                .map(c -> new ProcurementCenterDto(c.getId(), c.getName(), c.getLocation(), c.getDistrict(), c.getAddress(), c.isActive()))
                .toList();
    }

    public List<CropDto> listCrops() {
        return cropRepository.findAll().stream()
                                .filter(Crop::isActive)
                .map(c -> new CropDto(c.getId(), c.getName(), c.getDescription()))
                .toList();
    }

    @Transactional
    public BookingResponse createBooking(Long userId, BookingRequest request) {
        Farmer farmer = getFarmerByUserId(userId);

        ProcurementCenter center = procurementCenterRepository.findById(request.procurementCenterId())
                .orElseThrow(() -> new ResourceNotFoundException("Procurement center not found."));
        if (!center.isActive()) {
            throw new BadRequestException("This procurement center is not currently active.");
        }

        Crop crop = cropRepository.findById(request.cropId())
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found."));

        if (!request.date().isAfter(LocalDate.now().minusDays(1))) {
            throw new BadRequestException("Booking date must be today or later.");
        }

        Booking booking = Booking.builder()
                .bookingId("BK-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .farmer(farmer)
                .procurementCenter(center)
                .crop(crop)
                .date(request.date())
                .startTime(request.startTime())
                .endTime(request.startTime().plusMinutes(30))
                .status(BookingStatus.BOOKED)
                .quantityKg(request.quantityKg())
                .build();
        booking = bookingRepository.save(booking);

        Token token = queueService.issueToken(booking);

        notificationService.notify(farmer.getUser(), String.format(
                "Your slot for %s, %s at %s is confirmed. Token: %s.",
                booking.getDate().format(DATE_FORMAT), booking.getStartTime().format(TIME_FORMAT),
                center.getName(), token.getTokenNumber()));

        int waitMinutes = queueService.estimatedWaitMinutes(center.getId(), booking.getDate(), token.getQueuePosition());

        return new BookingResponse(
                booking.getBookingId(), token.getTokenNumber(), token.getQueuePosition(), waitMinutes,
                booking.getStatus().toDisplay(), center.getName(), crop.getName(),
                booking.getDate().format(DATE_FORMAT),
                booking.getStartTime().format(TIME_FORMAT) + " - " + booking.getEndTime().format(TIME_FORMAT));
    }

    public List<BookingResponse> listBookings(Long userId) {
        Farmer farmer = getFarmerByUserId(userId);
        return bookingRepository.findByFarmerIdOrderByDateDescCreatedAtDesc(farmer.getId()).stream()
                .map(this::toBookingResponse)
                .toList();
    }

    public BookingResponse getBooking(Long userId, String bookingId) {
        Farmer farmer = getFarmerByUserId(userId);
        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getFarmer().getId().equals(farmer.getId())) {
            throw new ResourceNotFoundException("Booking not found: " + bookingId);
        }

        return toBookingResponse(booking);
    }

    @Transactional
    public void cancelBooking(Long userId, String bookingId) {
        Farmer farmer = getFarmerByUserId(userId);
        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getFarmer().getId().equals(farmer.getId())) {
            throw new ResourceNotFoundException("Booking not found: " + bookingId);
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("A completed booking cannot be cancelled.");
        }

        booking.setStatus(BookingStatus.REJECTED);
        bookingRepository.save(booking);

        notificationService.notify(farmer.getUser(),
                "Your booking " + booking.getBookingId() + " has been cancelled.");
    }

    public List<ProcurementHistoryDto> getProcurementHistory(Long userId) {
        Farmer farmer = getFarmerByUserId(userId);
        return bookingRepository.findByFarmerIdOrderByDateDescCreatedAtDesc(farmer.getId()).stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED || b.getStatus() == BookingStatus.REJECTED)
                .map(this::toHistoryDto)
                .toList();
    }

    public List<PaymentDto> getPayments(Long userId) {
        Farmer farmer = getFarmerByUserId(userId);
        return paymentRepository.findByFarmerIdOrderByPaymentDateDesc(farmer.getId()).stream()
                .map(p -> new PaymentDto(
                        p.getId(), p.getAmount(), p.getPaymentDate().format(DATE_FORMAT),
                        p.getStatus(), p.getTransactionReference()))
                .toList();
    }

    private UpcomingBookingDto toUpcomingDto(Booking b) {
        return new UpcomingBookingDto(
                b.getBookingId(), b.getProcurementCenter().getName(), b.getCrop().getName(),
                b.getDate().format(DATE_FORMAT),
                b.getStartTime().format(TIME_FORMAT) + " - " + b.getEndTime().format(TIME_FORMAT),
                tokenNumberFor(b));
    }

    private QueueInfoDto buildQueueInfo(Booking b) {
        List<Token> queue = queueService.todaysQueue(b.getProcurementCenter().getId(), b.getDate());
        int position = queue.stream()
                .filter(t -> t.getBooking().getId().equals(b.getId()))
                .findFirst()
                .map(Token::getQueuePosition)
                .orElse(1);
        int wait = queueService.estimatedWaitMinutes(b.getProcurementCenter().getId(), b.getDate(), position);
        return new QueueInfoDto(position, queue.size(), wait, b.getStatus().toDisplay());
    }

    private BookingResponse toBookingResponse(Booking b) {
        List<Token> queue = queueService.todaysQueue(b.getProcurementCenter().getId(), b.getDate());
        Token token = queue.stream().filter(t -> t.getBooking().getId().equals(b.getId())).findFirst().orElse(null);
        int position = token != null ? token.getQueuePosition() : 0;
        int wait = queueService.estimatedWaitMinutes(b.getProcurementCenter().getId(), b.getDate(), position);

        return new BookingResponse(
                b.getBookingId(), token != null ? token.getTokenNumber() : "-", position, wait,
                b.getStatus().toDisplay(), b.getProcurementCenter().getName(), b.getCrop().getName(),
                b.getDate().format(DATE_FORMAT),
                b.getStartTime().format(TIME_FORMAT) + " - " + b.getEndTime().format(TIME_FORMAT));
    }

    private ProcurementHistoryDto toHistoryDto(Booking b) {
        BigDecimal amount = paymentRepository.findByFarmerIdOrderByPaymentDateDesc(b.getFarmer().getId()).stream()
                .filter(p -> p.getBooking() != null && p.getBooking().getId().equals(b.getId()))
                .map(Payment::getAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        return new ProcurementHistoryDto(
                b.getBookingId(), b.getDate().format(DATE_FORMAT), b.getCrop().getName(),
                b.getProcurementCenter().getName(),
                b.getQuantityKg() != null ? b.getQuantityKg() : 0,
                amount.doubleValue(), b.getStatus().toDisplay());
    }

    private String tokenNumberFor(Booking b) {
        return queueService.todaysQueue(b.getProcurementCenter().getId(), b.getDate()).stream()
                .filter(t -> t.getBooking().getId().equals(b.getId()))
                .findFirst()
                .map(Token::getTokenNumber)
                .orElse("-");
    }
}
