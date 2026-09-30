package com.spms.controller;

import com.spms.dto.OfficerDashboardResponse;
import com.spms.dto.QueueStatusUpdateRequest;
import com.spms.dto.QueueTableRowDto;
import com.spms.security.UserPrincipal;
import com.spms.service.OfficerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/officer")
@RequiredArgsConstructor
public class OfficerController {

    private final OfficerService officerService;

    @GetMapping("/dashboard")
    public ResponseEntity<OfficerDashboardResponse> dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(officerService.getDashboard(principal.getId()));
    }

    @GetMapping("/queue")
    public ResponseEntity<List<QueueTableRowDto>> queue(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(officerService.getQueue(principal.getId()));
    }

    @PostMapping("/queue/next")
    public ResponseEntity<QueueTableRowDto> callNext(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(officerService.callNext(principal.getId()));
    }

    @PutMapping("/queue/{tokenNumber}/status")
    public ResponseEntity<QueueTableRowDto> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String tokenNumber,
            @Valid @RequestBody QueueStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(officerService.updateStatus(principal.getId(), tokenNumber, request));
    }
}
