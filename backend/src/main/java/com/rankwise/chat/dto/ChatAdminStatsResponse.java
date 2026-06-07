package com.rankwise.chat.dto;

import java.util.List;
import java.util.Map;

public record ChatAdminStatsResponse(
        long totalSessions,
        long totalMessages,
        long totalChatUsers,
        long totalProfiles,
        long chatOpens,
        long messagesSent,
        long whatsappClicks,
        long predictorUsage,
        long comparisonUsage,
        List<Map<String, Object>> topEventTypes,
        List<Map<String, Object>> recentProfiles
) {
}
