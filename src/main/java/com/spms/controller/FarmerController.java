package com.spms.controller;

import com.spms.dto.*;
import com.spms.security.UserPrincipal;
import com.spms.service.FarmerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farmer")
@RequiredArgsConstructor
public class FarmerController {

    private final FarmerService farmerService;

    @GetMapping("/dashboard")
    public ResponseEntity<FarmerDashboardResponse> dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(farmerService.getDashboard(principal.getId()));
    }

    @GetMapping("/procurement-centers")
    public ResponseEntity<List<ProcurementCenterDto>> centers() {
        return ResponseEntity.ok(farmerService.listCenters());
    }

    @GetMapping("/crops")
    public ResponseEntity<List<CropDto>> crops() {
        return ResponseEntity.ok(farmerService.listCrops());
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BookingRequest request
    ) {
        BookingResponse response = farmerService.createBooking(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> listBookings(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(farmerService.listBookings(principal.getId()));
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> getBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id
    ) {
        return ResponseEntity.ok(farmerService.getBooking(principal.getId(), id));
    }

    @PutMapping("/bookings/{id}/cancel")
    public ResponseEntity<Void> cancelBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id
    ) {
        farmerService.cancelBooking(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/procurement-history")
    public ResponseEntity<List<ProcurementHistoryDto>> history(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(farmerService.getProcurementHistory(principal.getId()));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentDto>> payments(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(farmerService.getPayments(principal.getId()));
    }
}
