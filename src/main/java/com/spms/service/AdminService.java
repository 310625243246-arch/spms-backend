package com.spms.service;

import com.spms.dto.*;
import com.spms.entity.Booking;
import com.spms.entity.BookingStatus;
import com.spms.entity.Farmer;
import com.spms.entity.Officer;
import com.spms.entity.Payment;
import com.spms.entity.ProcurementCenter;
import com.spms.exception.ResourceNotFoundException;
import com.spms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final List<BookingStatus> PENDING_STATUSES =
            List.of(BookingStatus.BOOKED, BookingStatus.CHECKED_IN, BookingStatus.IN_QUEUE,
                    BookingStatus.GRADING, BookingStatus.WEIGHING, BookingStatus.PAYMENT_PENDING);

    private final FarmerRepository farmerRepository;
    private final OfficerRepository officerRepository;
    private final ProcurementCenterRepository procurementCenterRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public AdminDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();

        long totalFarmers = farmerRepository.count();
        long activeCenters = procurementCenterRepository.countByActiveTrue();
        long bookingsToday = bookingRepository.countByDate(today);
        long completed = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        long pending = bookingRepository.countByStatusIn(PENDING_STATUSES);
        double totalAmountPaid = paymentRepository.findAllByOrderByPaymentDateDesc().stream()
            .filter(payment -> "Completed".equalsIgnoreCase(payment.getStatus()))
            .mapToDouble(payment -> payment.getAmount().doubleValue())
            .sum();

        AdminStatsDto stats = new AdminStatsDto(totalFarmers, activeCenters, bookingsToday, completed, pending, totalAmountPaid);

        List<CenterActivityDto> centerActivity = procurementCenterRepository.findAll().stream()
                .map(center -> {
                    long centerBookingsToday = bookingRepository.countByProcurementCenterIdAndDate(center.getId(), today);
                    long centerCompleted = bookingRepository.countByProcurementCenterIdAndDateAndStatus(
                            center.getId(), today, BookingStatus.COMPLETED);
                    long centerPending = bookingRepository.countByProcurementCenterIdAndDateAndStatusIn(
                            center.getId(), today, PENDING_STATUSES);
                    return new CenterActivityDto(center.getName(), centerBookingsToday, centerCompleted, centerPending);
                })
                .toList();

        return new AdminDashboardResponse(stats, centerActivity);
    }

    public List<FarmerSummaryDto> listFarmers() {
        return farmerRepository.findAll().stream()
                .map(this::toFarmerSummary)
                .toList();
    }

    public List<OfficerSummaryDto> listOfficers() {
        return officerRepository.findAll().stream()
                .map(this::toOfficerSummary)
                .toList();
    }

    public List<BookingSummaryDto> listBookings() {
        return bookingRepository.findAll().stream()
                .map(this::toBookingSummary)
                .toList();
    }

    public List<ProcurementCenterDto> listCenters() {
        return procurementCenterRepository.findAll().stream()
                .map(this::toCenterDto)
                .toList();
    }

    @Transactional
    public ProcurementCenterDto createCenter(ProcurementCenterRequest request) {
        ProcurementCenter center = ProcurementCenter.builder()
                .name(request.name())
                .location(request.location())
                .district(request.district())
                .address(request.address())
                .active(request.active())
                .build();
        center = procurementCenterRepository.save(center);
        return toCenterDto(center);
    }

    @Transactional
    public ProcurementCenterDto updateCenter(Long id, ProcurementCenterRequest request) {
        ProcurementCenter center = procurementCenterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Procurement center not found: " + id));

        center.setName(request.name());
        center.setLocation(request.location());
        center.setDistrict(request.district());
        center.setAddress(request.address());
        center.setActive(request.active());

        center = procurementCenterRepository.save(center);
        return toCenterDto(center);
    }

    @Transactional
    public void deleteCenter(Long id) {
        if (!procurementCenterRepository.existsById(id)) {
            throw new ResourceNotFoundException("Procurement center not found: " + id);
        }
        procurementCenterRepository.deleteById(id);
    }

    private FarmerSummaryDto toFarmerSummary(Farmer f) {
        return new FarmerSummaryDto(f.getId(), f.getFarmerId(), f.getName(), f.getUser().getEmail(),
                f.getPhone(), f.getVillage(), f.getDistrict());
    }

    private OfficerSummaryDto toOfficerSummary(Officer o) {
        return new OfficerSummaryDto(o.getId(), o.getOfficerId(), o.getName(), o.getUser().getEmail(),
                o.getPhone(), o.getProcurementCenter() != null ? o.getProcurementCenter().getName() : "Unassigned");
    }

    private BookingSummaryDto toBookingSummary(Booking b) {
        return new BookingSummaryDto(b.getBookingId(), b.getFarmer().getName(), b.getProcurementCenter().getName(),
                b.getCrop().getName(), b.getDate().format(DATE_FORMAT), b.getStatus().toDisplay());
    }

    private ProcurementCenterDto toCenterDto(ProcurementCenter c) {
        return new ProcurementCenterDto(c.getId(), c.getName(), c.getLocation(), c.getDistrict(), c.getAddress(), c.isActive());
    }
}
