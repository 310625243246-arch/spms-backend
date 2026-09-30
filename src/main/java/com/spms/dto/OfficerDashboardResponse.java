package com.spms.dto;

import java.util.List;

public record OfficerDashboardResponse(
        String centerName,
        String date,
        OfficerStatsDto stats,
        List<QueueTableRowDto> queue
) {
}
