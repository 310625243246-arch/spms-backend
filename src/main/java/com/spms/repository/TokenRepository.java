package com.spms.repository;

import com.spms.entity.Token;
import com.spms.entity.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findByBookingId(Long bookingId);

    Optional<Token> findByTokenNumber(String tokenNumber);

    List<Token> findByBooking_ProcurementCenterIdAndBooking_DateOrderByQueuePositionAsc(Long centerId, java.time.LocalDate date);

    Optional<Token> findFirstByBooking_ProcurementCenterIdAndBooking_DateAndStatusOrderByQueuePositionAsc(
            Long centerId, java.time.LocalDate date, TokenStatus status);

    long countByBooking_ProcurementCenterIdAndBooking_Date(Long centerId, java.time.LocalDate date);
}
