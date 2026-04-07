package com.example.interviewgateway.repository;

import com.example.interviewgateway.model.Answer;
import com.example.interviewgateway.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    // Prevents double-scoring if user somehow sends ANSWER twice for same question
    boolean existsByQuestion(Question question);

    List<Answer> findByQuestionSessionIdOrderByQuestionDifficultyLevelAsc(Long sessionId);
}