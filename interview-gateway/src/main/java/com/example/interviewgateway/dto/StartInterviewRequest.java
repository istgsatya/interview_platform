package com.example.interviewgateway.dto;

import jakarta.validation.constraints.NotBlank;

public record StartInterviewRequest(
        @NotBlank String resumeText,
        @NotBlank String targetJdText
) {
}
