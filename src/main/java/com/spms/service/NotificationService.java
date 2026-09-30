package com.spms.service;

import com.spms.dto.NotificationDto;
import com.spms.entity.Notification;
import com.spms.entity.User;
import com.spms.exception.ResourceNotFoundException;
import com.spms.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH);

    private final NotificationRepository notificationRepository;

    public void notify(User user, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .readStatus(false)
                .build();
        notificationRepository.save(notification);
    }

    public List<NotificationDto> getForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public void markRead(Long notificationId, Long requestingUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getUser().getId().equals(requestingUserId)) {
            throw new ResourceNotFoundException("Notification not found: " + notificationId);
        }

        notification.setReadStatus(true);
        notificationRepository.save(notification);
    }

    private NotificationDto toDto(Notification n) {
        String timestamp = n.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .format(TIMESTAMP_FORMAT);
        return new NotificationDto(String.valueOf(n.getId()), n.getMessage(), timestamp, n.isReadStatus());
    }
}
