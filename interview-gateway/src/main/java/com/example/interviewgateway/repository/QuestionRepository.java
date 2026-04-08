package com.example.interviewgateway.repository;

import com.example.interviewgateway.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySessionIdOrderByDifficultyLevelAsc(Long sessionId);

    long countBySessionId(Long sessionId);

    long countBySessionIdAndIsAskedTrue(Long sessionId);

    @Query("SELECT q.session.user.resumeText FROM Question q WHERE q.id = :questionId")
    String getResumeTextByQuestionId(@Param("questionId") Long questionId);

    @Query("SELECT q.session.id FROM Question q WHERE q.id = :questionId")
    Long getSessionIdByQuestionId(@Param("questionId") Long questionId);
}