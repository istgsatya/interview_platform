package com.example.interviewgateway.service;

import com.example.interviewgateway.model.Question;
import com.example.interviewgateway.model.InterviewSession;
import com.example.interviewgateway.repository.QuestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class LlmQuestionGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(LlmQuestionGeneratorService.class);

    @Value("${llm.api.key:}")
    private String apiKey;

    @Value("${llm.api.url}")
    private String apiUrl;

    @Value("${llm.api.model}")
    private String apiModel;

    private final QuestionRepository questionRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LlmQuestionGeneratorService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<Question> generateAndSaveQuestions(InterviewSession session, String resumeText, String jdText) {
        log.info("Initiating Groq API call for session {}", session.getId());

        if (!StringUtils.hasText(apiKey)) {
            log.error("Missing llm.api.key. Configure GROQ_API_KEY/llm.api.key before starting interviews.");
            return generateFallbackQuestions(session, resumeText, jdText);
        }

        // 1. Setup the Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 2. Construct the Prompts
        String systemPrompt = "You are an expert technical interviewer. Analyze the provided Resume and Job Description. " +
                "Generate exactly 5 technical interview questions targeting the gap between the candidate's skills and the JD. " +
                "Sort them from easiest (1) to hardest (5). " +
                "You MUST return ONLY a raw JSON array of objects with this exact format: " +
                "[{\"difficulty\": 1, \"question\": \"...\", \"idealAnswer\": \"...\"}]";

        String userContext = "Resume: " + resumeText + "\n\nJob Description: " + jdText;

        // 3. SAFELY build the JSON payload using Java Maps (Jackson handles escaping)
        java.util.Map<String, Object> requestBodyMap = new java.util.HashMap<>();
        requestBodyMap.put("model", apiModel);

        java.util.Map<String, String> systemMessage = new java.util.HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        java.util.Map<String, String> userMessage = new java.util.HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userContext);

        requestBodyMap.put("messages", java.util.List.of(systemMessage, userMessage));
        requestBodyMap.put("temperature", 0.3);

        // Pass the Map instead of a raw String
        HttpEntity<java.util.Map<String, Object>> requestEntity = new HttpEntity<>(requestBodyMap, headers);

        try {
            // 4. Fire the request to Groq
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("Groq API returned non-success status {} for session {}", response.getStatusCode(), session.getId());
                return generateFallbackQuestions(session, resumeText, jdText);
            }

            // 5. Parse the response and save it
            return parseAndSaveQuestions(session, response.getBody());

        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Groq API unauthorized (401). Verify GROQ_API_KEY validity and project permissions. Response: {}", e.getResponseBodyAsString());
            return generateFallbackQuestions(session, resumeText, jdText);
        } catch (HttpClientErrorException e) {
            log.error("Groq API request failed with status {} and body {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return generateFallbackQuestions(session, resumeText, jdText);
        } catch (Exception e) {
            log.error("Failed to communicate with Groq API.", e);
            return generateFallbackQuestions(session, resumeText, jdText);
        }
    }

    private List<Question> parseAndSaveQuestions(InterviewSession session, String responseBody) throws Exception {
        List<Question> savedQuestions = new ArrayList<>();

        // Dig into the Grok JSON response to find the actual text message
        JsonNode root = objectMapper.readTree(responseBody);
        String rawContent = root.path("choices").get(0).path("message").path("content").asText();

        // Clean up markdown formatting if Grok wraps the JSON in ```json blocks
        String cleanJson = rawContent.replaceAll("```json", "").replaceAll("```", "").trim();

        // Parse the clean JSON array
        JsonNode questionsArray = objectMapper.readTree(cleanJson);

        for (JsonNode qNode : questionsArray) {
            Question q = new Question();
            q.setSession(session);
            q.setDifficultyLevel(qNode.path("difficulty").asInt());
            q.setQuestionText(qNode.path("question").asText());
            q.setIdealAnswer(qNode.path("idealAnswer").asText());
            q.setAsked(false);

            savedQuestions.add(questionRepository.save(q));
        }

    log.info("Successfully generated and saved {} dynamic questions from Groq.", savedQuestions.size());
        return savedQuestions;
    }

    private List<Question> generateFallbackQuestions(InterviewSession session, String resumeText, String jdText) {
    log.warn("Falling back to deterministic interview questions for session {}", session.getId());

    List<Question> fallbackQuestions = List.of(
        buildFallbackQuestion(session, 1,
            "Walk through one backend feature you built end-to-end and explain your API design decisions.",
            "A strong answer explains requirements, endpoint design, validation, persistence, error handling, and trade-offs."),
        buildFallbackQuestion(session, 2,
            "How would you optimize a slow database query in a production Spring application?",
            "A strong answer discusses query plans, indexing strategy, pagination, avoiding N+1 issues, and measuring impact."),
        buildFallbackQuestion(session, 3,
            "Design a robust authentication flow for a web app using JWT. What are common pitfalls?",
            "A strong answer covers token issuance, expiration, refresh flow, secure storage, revocation, and CSRF/XSS concerns."),
        buildFallbackQuestion(session, 4,
            "Given this JD context, which skill gap would you prioritize first and how would you close it in 30 days?",
            "A strong answer prioritizes one high-impact gap, outlines a realistic learning plan, and ties progress to measurable outcomes."),
        buildFallbackQuestion(session, 5,
            "How would you design and operate this system reliably under high concurrent interview traffic?",
            "A strong answer addresses scaling, caching, async processing, observability, fault tolerance, and incident response.")
    );

    return questionRepository.saveAll(fallbackQuestions);
    }

    private Question buildFallbackQuestion(InterviewSession session, int difficulty, String questionText, String idealAnswer) {
    Question q = new Question();
    q.setSession(session);
    q.setDifficultyLevel(difficulty);
    q.setQuestionText(questionText);
    q.setIdealAnswer(idealAnswer);
    q.setAsked(false);
    return q;
    }
}