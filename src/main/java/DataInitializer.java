package com.spms;

import com.spms.entity.Role;
import com.spms.entity.User;
import com.spms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (userRepository.findByEmail("OFC-101").isEmpty()) {

            User officer = User.builder()
                    .name("Demo Officer")
                    .email("OFC-101")
                    .phone("9999999999")
                    .password(passwordEncoder.encode("demo123"))
                    .role(Role.OFFICER)
                    .build();

            userRepository.save(officer);

            System.out.println("=================================");
            System.out.println("DEMO OFFICER CREATED");
            System.out.println("Username: OFC-101");
            System.out.println("Password: demo123");
            System.out.println("=================================");
        }
    }
}