package com.spms.dto;

import java.util.List;

public record AdminDashboardResponse(
        AdminStatsDto stats,
        List<CenterActivityDto> centerActivity
) {
}
