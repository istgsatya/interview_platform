package com.example.interviewgateway.dto;

public record StartInterviewResponse(
        Long sessionId,
        int questionCount,
        String status
) {
}
