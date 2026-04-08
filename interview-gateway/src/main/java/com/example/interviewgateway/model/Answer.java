package com.example.interviewgateway.model;

import jakarta.persistence.*;

@Entity
@Table(name = "answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(columnDefinition = "TEXT")
    private String candidateText;

    private Double mlSimilarityScore;
    private Double mlLengthScore;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getFinalAggregateScore() {
        return finalAggregateScore;
    }

    public void setFinalAggregateScore(Double finalAggregateScore) {
        this.finalAggregateScore = finalAggregateScore;
    }

    public Double getMlLengthScore() {
        return mlLengthScore;
    }

    public void setMlLengthScore(Double mlLengthScore) {
        this.mlLengthScore = mlLengthScore;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public String getCandidateText() {
        return candidateText;
    }

    public void setCandidateText(String candidateText) {
        this.candidateText = candidateText;
    }

    public Double getMlSimilarityScore() {
        return mlSimilarityScore;
    }

    public void setMlSimilarityScore(Double mlSimilarityScore) {
        this.mlSimilarityScore = mlSimilarityScore;
    }

    public String getLlmFeedback() {
        return llmFeedback;
    }

    public void setLlmFeedback(String llmFeedback) {
        this.llmFeedback = llmFeedback;
    }

    private Double finalAggregateScore;

    @Column(columnDefinition = "TEXT")
    private String llmFeedback;

    // TODO: Press Alt+Insert to generate Getters and Setters
}