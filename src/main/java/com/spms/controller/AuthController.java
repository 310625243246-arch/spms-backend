package com.spms.controller;

import com.spms.dto.LoginRequest;
import com.spms.dto.LoginResponse;
import com.spms.dto.RegisterFarmerRequest;
import com.spms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/farmer/register")
    public ResponseEntity<Void> registerFarmer(@Valid @RequestBody RegisterFarmerRequest request) {
        authService.registerFarmer(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
