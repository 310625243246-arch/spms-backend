package com.spms.repository;

import com.spms.entity.ProcurementCenter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementCenterRepository extends JpaRepository<ProcurementCenter, Long> {
    long countByActiveTrue();
}
