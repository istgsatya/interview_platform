package com.example.interviewgateway.dto;

public record TranscriptItemResponse(
        Long questionId,
        Integer difficultyLevel,
        String questionText,
        String idealAnswer,
        String candidateText,
        Double mlSimilarityScore,
        Double mlLengthScore,
        Double finalAggregateScore,
        String llmFeedback,
        boolean asked
) {
}
