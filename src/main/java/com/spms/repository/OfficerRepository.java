package com.spms.repository;

import com.spms.entity.Officer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfficerRepository extends JpaRepository<Officer, Long> {
    Optional<Officer> findByUserId(Long userId);
    Optional<Officer> findByUserEmail(String email);
    long count();
}
