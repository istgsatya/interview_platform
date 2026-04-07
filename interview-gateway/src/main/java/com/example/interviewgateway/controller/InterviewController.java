package com.example.interviewgateway.controller;

import com.example.interviewgateway.dto.StartInterviewRequest;
import com.example.interviewgateway.dto.StartInterviewResponse;
import com.example.interviewgateway.dto.SessionSummaryResponse;
import com.example.interviewgateway.dto.TranscriptItemResponse;
import com.example.interviewgateway.dto.TranscriptResponse;
import com.example.interviewgateway.model.Answer;
import com.example.interviewgateway.model.InterviewSession;
import com.example.interviewgateway.model.Question;
import com.example.interviewgateway.model.User;
import com.example.interviewgateway.repository.AnswerRepository;
import com.example.interviewgateway.repository.InterviewSessionRepository;
import com.example.interviewgateway.repository.QuestionRepository;
import com.example.interviewgateway.repository.UserRepository;
import com.example.interviewgateway.service.LlmQuestionGeneratorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final LlmQuestionGeneratorService llmService;
    private final UserRepository userRepository;
    private final InterviewSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    public InterviewController(LlmQuestionGeneratorService llmService,
                               UserRepository userRepository,
                               InterviewSessionRepository sessionRepository,
                               QuestionRepository questionRepository,
                               AnswerRepository answerRepository) {
        this.llmService = llmService;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startInterview(@Valid @RequestBody StartInterviewRequest request,
                                            Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authenticated user not found");
        }

        user.setResumeText(request.resumeText().trim());
        user.setTargetJdText(request.targetJdText().trim());
        userRepository.save(user);

        InterviewSession session = new InterviewSession();
        session.setUser(user);
        session.setStatus("IN_PROGRESS");
        session = sessionRepository.save(session);

        List<Question> questions = llmService.generateAndSaveQuestions(
                session,
                user.getResumeText(),
                user.getTargetJdText()
        );

        return ResponseEntity.ok(
                new StartInterviewResponse(session.getId(), questions.size(), session.getStatus())
        );
    }

    @GetMapping("/sessions/{sessionId}/transcript")
    public ResponseEntity<?> getSessionTranscript(@PathVariable Long sessionId, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authenticated user not found");
        }

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId()).orElse(null);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session not found for this user");
        }

        List<Question> questions = questionRepository.findBySessionIdOrderByDifficultyLevelAsc(sessionId);
        List<Answer> answers = answerRepository.findByQuestionSessionIdOrderByQuestionDifficultyLevelAsc(sessionId);

        Map<Long, Answer> answerByQuestionId = new HashMap<>();
        for (Answer answer : answers) {
            answerByQuestionId.put(answer.getQuestion().getId(), answer);
        }

        List<TranscriptItemResponse> items = questions.stream().map(question -> {
            Answer answer = answerByQuestionId.get(question.getId());
            return new TranscriptItemResponse(
                    question.getId(),
                    question.getDifficultyLevel(),
                    question.getQuestionText(),
                    question.getIdealAnswer(),
                    answer != null ? answer.getCandidateText() : null,
                    answer != null ? answer.getMlSimilarityScore() : null,
                    answer != null ? answer.getMlLengthScore() : null,
                    answer != null ? answer.getFinalAggregateScore() : null,
                    answer != null ? answer.getLlmFeedback() : null,
                    question.isAsked()
            );
        }).toList();

        return ResponseEntity.ok(
                new TranscriptResponse(session.getId(), session.getStatus(), session.getCreatedAt(), items)
        );
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> listUserSessions(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authenticated user not found");
        }

        List<InterviewSession> sessions = sessionRepository.findByUserId(user.getId());

        List<SessionSummaryResponse> summary = sessions.stream()
                .sorted(Comparator.comparing(InterviewSession::getCreatedAt).reversed())
                .map(session -> {
                    long questionCount = questionRepository.countBySessionId(session.getId());
                    long answeredCount = questionRepository.countBySessionIdAndIsAskedTrue(session.getId());
                    List<Answer> answers = answerRepository.findByQuestionSessionIdOrderByQuestionDifficultyLevelAsc(session.getId());
                    double averageScore = answers.stream()
                            .map(Answer::getFinalAggregateScore)
                            .filter(score -> score != null)
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0.0);

                    return new SessionSummaryResponse(
                            session.getId(),
                            session.getStatus(),
                            session.getCreatedAt(),
                            questionCount,
                            answeredCount,
                            Math.round(averageScore * 100.0) / 100.0
                    );
                })
                .toList();

        return ResponseEntity.ok(summary);
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