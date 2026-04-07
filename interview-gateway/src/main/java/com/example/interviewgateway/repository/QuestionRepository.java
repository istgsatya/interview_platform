package com.example.interviewgateway.repository;

import com.example.interviewgateway.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySessionIdOrderByDifficultyLevelAsc(Long sessionId);

    long countBySessionId(Long sessionId);

    long countBySessionIdAndIsAskedTrue(Long sessionId);
}