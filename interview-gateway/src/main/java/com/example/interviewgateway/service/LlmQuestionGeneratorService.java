package com.example.interviewgateway.service;

import com.example.interviewgateway.model.Question;
import com.example.interviewgateway.model.InterviewSession;
import com.example.interviewgateway.repository.QuestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class LlmQuestionGeneratorService {

   // @Value("${llm.api.key}")
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
        System.out.println("Initiating real Grok API call for Session: " + session.getId());

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
            // 4. Fire the request to Grok
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

            // 5. Parse the response and save it
            return parseAndSaveQuestions(session, response.getBody());

        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to communicate with Grok API.");
            e.printStackTrace();
            return new ArrayList<>();
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

        System.out.println("Successfully generated and saved " + savedQuestions.size() + " dynamic questions from Grok.");
        return savedQuestions;
    }
}