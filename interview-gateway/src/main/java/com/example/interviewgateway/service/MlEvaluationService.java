package com.example.interviewgateway.service;

import com.example.interviewgateway.dto.EvaluationResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class MlEvaluationService {

    private final RestTemplate restTemplate;
    // Hardcoding the local Python URL for now. We will move this to properties later.
    private final String pythonMlUrl = "http://localhost:8000/evaluate";

    public MlEvaluationService() {
        this.restTemplate = new RestTemplate();
    }

    public EvaluationResponse evaluateAnswer(String candidateAnswer, String idealAnswer) {
        System.out.println("--- Sending Answer to Python ML Engine ---");

        // 1. Setup the Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. Build the exact JSON payload the Python FastAPI server expects
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("candidate_answer", candidateAnswer);
        requestBody.put("ideal_answer", idealAnswer);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 3. Fire the request to Port 8000 and map the JSON response directly to our Record
            ResponseEntity<EvaluationResponse> response = restTemplate.postForEntity(
                    pythonMlUrl,
                    requestEntity,
                    EvaluationResponse.class
            );

            System.out.println("Successfully received score from Python: " + response.getBody().score());
            return response.getBody();

        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to communicate with Python ML Engine.");
            e.printStackTrace();
            // Return a fail-safe empty evaluation if the Python server crashes
            return new EvaluationResponse(0.0, 0.0, 0.0, java.util.List.of("Python ML Server Offline"));
        }
    }
}