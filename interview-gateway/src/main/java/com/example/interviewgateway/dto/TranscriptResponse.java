package com.example.interviewgateway.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TranscriptResponse(
        Long sessionId,
        String status,
        LocalDateTime createdAt,
        List<TranscriptItemResponse> items
) {
}
