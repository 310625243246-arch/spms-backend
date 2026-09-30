package com.spms.controller;

import com.spms.dto.NotificationDto;
import com.spms.security.UserPrincipal;
import com.spms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getForUser(principal.getId()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        notificationService.markRead(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
