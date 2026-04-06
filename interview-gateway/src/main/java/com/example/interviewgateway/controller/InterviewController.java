package com.example.interviewgateway.controller;

import com.example.interviewgateway.model.InterviewSession;
import com.example.interviewgateway.model.Question;
import com.example.interviewgateway.model.User;
import com.example.interviewgateway.repository.InterviewSessionRepository;
import com.example.interviewgateway.repository.UserRepository;
import com.example.interviewgateway.service.LlmQuestionGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final LlmQuestionGeneratorService llmService;
    private final UserRepository userRepository;
    private final InterviewSessionRepository sessionRepository;

    public InterviewController(LlmQuestionGeneratorService llmService, UserRepository userRepository, InterviewSessionRepository sessionRepository) {
        this.llmService = llmService;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    // A temporary endpoint to test the full pipeline without a frontend
    @PostMapping("/test-generate")
    public ResponseEntity<List<Question>> testGenerateQuestions() {

        System.out.println("--- Starting Test Generation Pipeline ---");

        // 1. Create a dummy user if one doesn't exist in the DB yet
        User user = userRepository.findByEmail("test@linux.com").orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail("test@linux.com");
            newUser.setPasswordHash("dummy_hash"); // We will do real JWT auth later

            // The AI will find the gap between these two strings
            newUser.setResumeText("Junior developer. I know Java, basic Spring Boot, and PostgreSQL. I have built a few REST APIs.");
            newUser.setTargetJdText("Senior DevOps & Backend Engineer. Requires advanced Spring Boot microservices, Docker containerization, Kubernetes, and AWS deployment experience.");

            return userRepository.save(newUser);
        });

        // 2. Create a new Interview Session for this user
        InterviewSession session = new InterviewSession();
        session.setUser(user);
        session.setStatus("PENDING");
        session = sessionRepository.save(session);

        // 3. Fire the Grok API service!
        List<Question> questions = llmService.generateAndSaveQuestions(session, user.getResumeText(), user.getTargetJdText());

        // 4. Return the generated questions as JSON
        return ResponseEntity.ok(questions);
    }
}