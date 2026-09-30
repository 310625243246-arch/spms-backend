package com.spms.controller;

import com.spms.dto.*;
import com.spms.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> dashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/farmers")
    public ResponseEntity<List<FarmerSummaryDto>> farmers() {
        return ResponseEntity.ok(adminService.listFarmers());
    }

    @GetMapping("/officers")
    public ResponseEntity<List<OfficerSummaryDto>> officers() {
        return ResponseEntity.ok(adminService.listOfficers());
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingSummaryDto>> bookings() {
        return ResponseEntity.ok(adminService.listBookings());
    }

    @GetMapping("/procurement-centers")
    public ResponseEntity<List<ProcurementCenterDto>> centers() {
        return ResponseEntity.ok(adminService.listCenters());
    }

    @PostMapping("/procurement-centers")
    public ResponseEntity<ProcurementCenterDto> createCenter(@Valid @RequestBody ProcurementCenterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCenter(request));
    }

    @PutMapping("/procurement-centers/{id}")
    public ResponseEntity<ProcurementCenterDto> updateCenter(
            @PathVariable Long id,
            @Valid @RequestBody ProcurementCenterRequest request
    ) {
        return ResponseEntity.ok(adminService.updateCenter(id, request));
    }

    @DeleteMapping("/procurement-centers/{id}")
    public ResponseEntity<Void> deleteCenter(@PathVariable Long id) {
        adminService.deleteCenter(id);
        return ResponseEntity.noContent().build();
    }
}
