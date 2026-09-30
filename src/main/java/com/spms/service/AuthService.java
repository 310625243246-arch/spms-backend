package com.spms.service;

import com.spms.dto.LoginRequest;
import com.spms.dto.LoginResponse;
import com.spms.dto.RegisterFarmerRequest;
import com.spms.dto.UserDto;
import com.spms.entity.Farmer;
import com.spms.entity.Officer;
import com.spms.entity.Role;
import com.spms.entity.User;
import com.spms.exception.BadRequestException;
import com.spms.exception.DuplicateResourceException;
import com.spms.repository.FarmerRepository;
import com.spms.repository.OfficerRepository;
import com.spms.repository.UserRepository;
import com.spms.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final OfficerRepository officerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Resolves the login "email" field as a flexible identifier: a real email,
     * a Farmer ID, an Officer ID, or a phone number. See LoginRequest's javadoc
     * for why - it keeps the existing frontend login screens unchanged.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = resolveUser(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        UserDto userDto = new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
        return new LoginResponse(token, userDto);
    }

    private Optional<User> resolveUser(String identifier) {
        Optional<User> byEmail = userRepository.findByEmail(identifier);
        if (byEmail.isPresent()) return byEmail;

        Optional<User> byFarmerId = farmerRepository.findAll().stream()
                .filter(f -> f.getFarmerId().equalsIgnoreCase(identifier))
                .map(Farmer::getUser)
                .findFirst();
        if (byFarmerId.isPresent()) return byFarmerId;

        Optional<User> byOfficerId = officerRepository.findAll().stream()
                .filter(o -> o.getOfficerId().equalsIgnoreCase(identifier))
                .map(Officer::getUser)
                .findFirst();
        if (byOfficerId.isPresent()) return byOfficerId;

        String normalizedPhone = identifier.replaceAll("\\s+", "");
        return userRepository.findAll().stream()
                .filter(u -> u.getPhone() != null && u.getPhone().replaceAll("\\s+", "").equals(normalizedPhone))
                .findFirst();
    }

    @Transactional
    public void registerFarmer(RegisterFarmerRequest request) {
        if (farmerRepository.existsByFarmerId(request.farmerId())) {
            throw new DuplicateResourceException("A farmer with this Farmer ID already exists.");
        }

        String syntheticEmail = (request.farmerId().trim().toLowerCase() + "@spms.local");
        if (userRepository.existsByEmail(syntheticEmail)) {
            throw new DuplicateResourceException("A farmer with this Farmer ID already exists.");
        }

        if (request.password() == null || request.password().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters.");
        }

        User user = User.builder()
                .name(request.fullName())
                .email(syntheticEmail)
                .phone(request.mobile())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.FARMER)
                .build();
        user = userRepository.save(user);

        Farmer farmer = Farmer.builder()
                .farmerId(request.farmerId())
                .user(user)
                .name(request.fullName())
                .phone(request.mobile())
                .email(syntheticEmail)
                .village(request.village())
                .district(request.district())
                .address(request.village() + ", " + request.district())
                .build();
        farmerRepository.save(farmer);
    }
}
