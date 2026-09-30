package com.spms;

import com.spms.entity.Officer;
import com.spms.entity.ProcurementCenter;
import com.spms.entity.Role;
import com.spms.entity.User;
import com.spms.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void run(String... args) {

        // =========================================================
        // 1. DEMO ADMIN USER
        // =========================================================

        if (userRepository.findByEmail("admin@spms.gov.in").isEmpty()) {

            User admin = User.builder()
                    .name("SPMS Admin")
                    .email("admin@spms.gov.in")
                    .phone("9999999998")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(admin);

            System.out.println("=================================");
            System.out.println("DEMO ADMIN CREATED");
            System.out.println("Email: admin@spms.gov.in");
            System.out.println("Password: admin123");
            System.out.println("=================================");
        }

        // =========================================================
        // 2. DEMO OFFICER USER
        // =========================================================

        User officerUser = userRepository.findByEmail("OFC-101")
                .orElseGet(() -> {

                    User officer = User.builder()
                            .name("Demo Officer")
                            .email("OFC-101")
                            .phone("9999999999")
                            .password(passwordEncoder.encode("demo123"))
                            .role(Role.OFFICER)
                            .build();

                    return userRepository.save(officer);
                });

        // =========================================================
        // 3. DEMO PROCUREMENT CENTER
        // =========================================================

        ProcurementCenter procurementCenter = entityManager
                .createQuery(
                        "SELECT p FROM ProcurementCenter p WHERE p.name = :name",
                        ProcurementCenter.class
                )
                .setParameter("name", "SPMS Demo Procurement Center")
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {

                    ProcurementCenter center = ProcurementCenter.builder()
                            .name("SPMS Demo Procurement Center")
                            .location("Demo Location")
                            .district("Demo District")
                            .address("SPMS Demo Center")
                            .active(true)
                            .build();

                    entityManager.persist(center);
                    return center;
                });

        // =========================================================
        // 4. DEMO OFFICER PROFILE
        // =========================================================

        Long officerCount = entityManager.createQuery(
                "SELECT COUNT(o) FROM Officer o WHERE o.officerId = :officerId",
                Long.class
        )
        .setParameter("officerId", "OFC-101")
        .getSingleResult();

        if (officerCount == 0) {

            Officer officerProfile = Officer.builder()
                    .officerId("OFC-101")
                    .user(officerUser)
                    .name("Demo Officer")
                    .phone("9999999999")
                    .email("OFC-101")
                    .procurementCenter(procurementCenter)
                    .build();

            entityManager.persist(officerProfile);

            System.out.println("=================================");
            System.out.println("DEMO OFFICER PROFILE CREATED");
            System.out.println("Officer ID: OFC-101");
            System.out.println("Procurement Center: SPMS Demo Procurement Center");
            System.out.println("=================================");
        }
    }
}