package com.example.interviewgateway.dto;

import java.util.List;

// Records automatically generate getters, setters, and constructors behind the scenes!
public record EvaluationResponse(
        double score,
        double similarity_score,
        double length_ratio,
        List<String> flags
) {}