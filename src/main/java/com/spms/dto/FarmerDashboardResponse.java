package com.spms.dto;

import java.util.List;

public record FarmerDashboardResponse(
        FarmerProfileDto farmer,
        UpcomingBookingDto upcomingBooking,
        QueueInfoDto queueInfo,
        List<NotificationDto> notifications,
        List<ProcurementHistoryDto> history
) {
}
