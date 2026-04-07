package com.example.interviewgateway.dto;

import java.time.LocalDateTime;

public record SessionSummaryResponse(
        Long sessionId,
        String status,
        LocalDateTime createdAt,
        long questionCount,
        long answeredCount,
        Double averageScore
) {
}
