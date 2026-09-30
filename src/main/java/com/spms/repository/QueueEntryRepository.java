package com.spms.repository;

import com.spms.entity.QueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {
    Optional<QueueEntry> findByProcurementCenterIdAndDate(Long centerId, LocalDate date);
}
