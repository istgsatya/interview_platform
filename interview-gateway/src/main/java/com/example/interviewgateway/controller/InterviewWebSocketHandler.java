package com.example.interviewgateway.controller;

import com.example.interviewgateway.dto.EvaluationResponse;
import com.example.interviewgateway.model.Answer;
import com.example.interviewgateway.model.Question;
import com.example.interviewgateway.repository.AnswerRepository;
import com.example.interviewgateway.repository.QuestionRepository;
import com.example.interviewgateway.service.MlEvaluationService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InterviewWebSocketHandler extends TextWebSocketHandler {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final MlEvaluationService mlService;

    private final ConcurrentHashMap<String, Question> activeQuestions = new ConcurrentHashMap<>();

    public InterviewWebSocketHandler(QuestionRepository questionRepository,
                                     AnswerRepository answerRepository,
                                     MlEvaluationService mlService) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.mlService = mlService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.sendMessage(new TextMessage(
                "SYSTEM: Connected! Send 'START:YOUR_SESSION_ID' to begin. (Example: START:6)"
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        if (payload.startsWith("START:")) {
            Long interviewSessionId = Long.parseLong(payload.split(":")[1].trim());
            askNextQuestion(session, interviewSessionId);

        } else if (payload.startsWith("ANSWER:")) {
            String candidateAnswer = payload.substring(7).trim();
            Question currentQ = activeQuestions.get(session.getId());

            if (currentQ == null) {
                session.sendMessage(new TextMessage(
                        "SYSTEM: No active question. Type START:ID to begin."
                ));
                return;
            }

            // ── Guard: prevent double-submission for same question ──────────
            if (answerRepository.existsByQuestion(currentQ)) {
                session.sendMessage(new TextMessage(
                        "SYSTEM: This question was already answered. Moving to next..."
                ));
                askNextQuestion(session, currentQ.getSession().getId());
                return;
            }

            session.sendMessage(new TextMessage(
                    "SYSTEM: Evaluating your answer using Python ML Engine..."
            ));

            // ── Step 1: Call Python ML engine ───────────────────────────────
            EvaluationResponse eval = mlService.evaluateAnswer(
                    candidateAnswer,
                    currentQ.getIdealAnswer()
            );

            // ── Step 2: Mark question as asked ──────────────────────────────
            currentQ.setAsked(true);
            questionRepository.save(currentQ);

            // ── Step 3: ✅ THE FIX — Persist the Answer to the database ─────
            Answer answer = new Answer();
            answer.setQuestion(currentQ);
            answer.setCandidateText(candidateAnswer);           // matches Answer.java field
            answer.setMlSimilarityScore(eval.similarity_score()); // matches Answer.java field
            answer.setMlLengthScore(eval.length_ratio());         // matches Answer.java field
            answer.setFinalAggregateScore(eval.score());          // matches Answer.java field
            // Build a readable feedback string from the flags list
            String feedbackText = eval.flags().isEmpty()
                    ? "No issues detected."
                    : String.join(" | ", eval.flags());
            answer.setLlmFeedback(feedbackText);                  // matches Answer.java field
            answerRepository.save(answer); // ← THE LINE THAT WAS MISSING

            // ── Step 4: Stream result back to frontend instantly ────────────
            session.sendMessage(new TextMessage(
                    String.format(
                            ">> AI EVALUATION: %.2f/10 | Similarity: %.2f | Feedback: %s",
                            eval.score(),
                            eval.similarity_score(),
                            feedbackText
                    )
            ));

            // ── Step 5: Ask the next question ───────────────────────────────
            askNextQuestion(session, currentQ.getSession().getId());
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
            session.sendMessage(new TextMessage(
                    "\nQUESTION (Level " + nextQ.getDifficultyLevel() + "): " + nextQ.getQuestionText()
            ));
        } else {
            session.sendMessage(new TextMessage(
                    "\nSYSTEM: Interview Complete! You survived the gauntlet."
            ));
            activeQuestions.remove(session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        activeQuestions.remove(session.getId());
    }
}