package com.spms.dto;

public record QueueInfoDto(
        int queuePosition,
        int totalInQueue,
        int estimatedWaitMinutes,
        String status
) {
}
