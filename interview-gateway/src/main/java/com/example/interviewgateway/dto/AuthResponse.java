package com.example.interviewgateway.dto;

public record AuthResponse(
        String token,
        String email,
        String message
) {
}
