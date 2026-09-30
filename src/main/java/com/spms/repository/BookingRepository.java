package com.spms.repository;

import com.spms.entity.Booking;
import com.spms.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingId(String bookingId);

    List<Booking> findByFarmerIdOrderByDateDescCreatedAtDesc(Long farmerId);

    List<Booking> findByProcurementCenterIdAndDateOrderByStartTimeAsc(Long centerId, LocalDate date);

    long countByProcurementCenterIdAndDate(Long centerId, LocalDate date);

    long countByProcurementCenterIdAndDateAndStatus(Long centerId, LocalDate date, BookingStatus status);

    long countByProcurementCenterIdAndDateAndStatusIn(Long centerId, LocalDate date, List<BookingStatus> statuses);

    long countByDate(LocalDate date);

    long countByStatus(BookingStatus status);

    long countByStatusIn(List<BookingStatus> statuses);
}
