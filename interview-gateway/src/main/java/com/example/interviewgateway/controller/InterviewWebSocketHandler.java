package com.example.interviewgateway.controller;

import com.example.interviewgateway.dto.EvaluationResponse;
import com.example.interviewgateway.model.Answer;
import com.example.interviewgateway.model.Question;
import com.example.interviewgateway.repository.AnswerRepository;
import com.example.interviewgateway.repository.QuestionRepository;
import com.example.interviewgateway.service.MlEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InterviewWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(InterviewWebSocketHandler.class);

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final MlEvaluationService mlService;

    private final ConcurrentHashMap<String, Question> activeQuestions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> activeInterviewSessions = new ConcurrentHashMap<>();

    public InterviewWebSocketHandler(QuestionRepository questionRepository,
                                     AnswerRepository answerRepository,
                                     MlEvaluationService mlService) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.mlService = mlService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        safeSend(session, new TextMessage(
                "SYSTEM: Connected! Send 'START:YOUR_SESSION_ID' to begin. (Example: START:6)"
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        if (payload.startsWith("START:")) {
            Long interviewSessionId;
            try {
                interviewSessionId = Long.parseLong(payload.split(":")[1].trim());
            } catch (Exception ex) {
                safeSend(session, new TextMessage("SYSTEM: Invalid START command. Use START:<SESSION_ID>."));
                return;
            }

            activeInterviewSessions.put(session.getId(), interviewSessionId);
            askNextQuestion(session, interviewSessionId);

        } else if (payload.startsWith("ANSWER:")) {
            String candidateAnswer = payload.substring(7).trim();
            Question currentQ = activeQuestions.get(session.getId());
            Long interviewSessionId = activeInterviewSessions.get(session.getId());

            if (currentQ == null || interviewSessionId == null) {
                safeSend(session, new TextMessage(
                        "SYSTEM: No active question. Type START:ID to begin."
                ));
                return;
            }

            // ── Guard: prevent double-submission for same question ──────────
            if (answerRepository.existsByQuestion(currentQ)) {
                safeSend(session, new TextMessage(
                        "SYSTEM: This question was already answered. Moving to next..."
                ));
                askNextQuestion(session, interviewSessionId);
                return;
            }

            safeSend(session, new TextMessage(
                    "SYSTEM: Evaluating your answer using Python ML Engine..."
            ));

            // ── Step 1: Call Python ML engine WITH RESUME CONTEXT ───────────
            ///String userContext = currentQ.getSession().getUser().getResumeText(); // ✅ GRABBING CONTEXT
            // ── Step 1: Call Python ML engine WITH RESUME CONTEXT ───────────
            String userContext = questionRepository.getResumeTextByQuestionId(currentQ.getId()); // ✅ BULLETPROOF FETCH
            EvaluationResponse eval = mlService.evaluateAnswer(
                    candidateAnswer,
                    currentQ.getIdealAnswer(),
                    userContext                                                   // ✅ PASSING IT DOWN
            );

            // ── Step 2: Mark question as asked ──────────────────────────────
            currentQ.setAsked(true);
            questionRepository.save(currentQ);

            // ── Step 3: Persist the Answer to the database ──────────────────
            Answer answer = new Answer();
            answer.setQuestion(currentQ);
            answer.setCandidateText(candidateAnswer);
            answer.setMlSimilarityScore(eval.similarity_score());
            answer.setMlLengthScore(eval.length_ratio());
            answer.setFinalAggregateScore(eval.score());

            String feedbackText = eval.flags().isEmpty()
                    ? "No issues detected."
                    : String.join(" | ", eval.flags());
            answer.setLlmFeedback(feedbackText);
            answerRepository.save(answer);

            // ── Step 4: Stream result back to frontend instantly ────────────
            safeSend(session, new TextMessage(
                    String.format(
                            ">> AI EVALUATION: %.2f/10 | Similarity: %.2f | Feedback: %s",
                            eval.score(),
                            eval.similarity_score(),
                            feedbackText
                    )
            ));

            // ── Step 5: Ask the next question ───────────────────────────────
            askNextQuestion(session, interviewSessionId);
        }
    }

    private void askNextQuestion(WebSocketSession session, Long interviewSessionId) throws Exception {
        List<Question> pending = questionRepository
                .findBySessionIdOrderByDifficultyLevelAsc(interviewSessionId);

        Question nextQ = pending.stream()
                .filter(q -> !q.isAsked())
                .findFirst()
                .orElse(null);

        if (nextQ != null) {
            activeQuestions.put(session.getId(), nextQ);
            safeSend(session, new TextMessage(
                    "\nQUESTION (Level " + nextQ.getDifficultyLevel() + "): " + nextQ.getQuestionText()
            ));
        } else {
            safeSend(session, new TextMessage(
                    "\nSYSTEM: Interview Complete! You survived the gauntlet."
            ));
            activeQuestions.remove(session.getId());
            activeInterviewSessions.remove(session.getId());
        }
    }

    private void safeSend(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            log.debug("Skipping websocket send because session {} is closed", session.getId());
            return;
        }

        try {
            session.sendMessage(message);
        } catch (IOException | IllegalStateException e) {
            log.warn("WebSocket send failed for session {}: {}", session.getId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        activeQuestions.remove(session.getId());
        activeInterviewSessions.remove(session.getId());
    }
}