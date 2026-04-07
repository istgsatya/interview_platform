package com.example.interviewgateway.repository;

import com.example.interviewgateway.model.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    List<InterviewSession> findByUserId(Long userId);

    Optional<InterviewSession> findByIdAndUserId(Long id, Long userId);
}