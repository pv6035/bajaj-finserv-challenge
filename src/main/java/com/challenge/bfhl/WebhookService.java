package com.challenge.bfhl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@EnableRetry
public class WebhookService {
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    private static final String GENERATE_WEBHOOK_URL = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook";
    private static final String REG_NO = "REG12347";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WebhookService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public void processWebhook() {
        try {
            // Step 1: Generate webhook
            WebhookResponse webhookResponse = generateWebhook();
            
            // Step 2: Process data based on regNo
            JsonNode result = processData(webhookResponse);
            
            // Step 3: Send result to webhook
            sendResultToWebhook(webhookResponse.getWebhook(), webhookResponse.getAccessToken(), result);
            
        } catch (Exception e) {
            logger.error("Error processing webhook: {}", e.getMessage(), e);
        }
    }

    private WebhookResponse generateWebhook() throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("name", "John Doe");
        requestBody.put("regNo", REG_NO);
        requestBody.put("email", "john@example.com");

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        
        logger.info("Making request to generate webhook");
        ResponseEntity<String> response = restTemplate.exchange(
                GENERATE_WEBHOOK_URL,
                HttpMethod.POST,
                entity,
                String.class
        );

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        
        String webhook = responseJson.get("webhook").asText();
        String accessToken = responseJson.get("accessToken").asText();
        JsonNode data = responseJson.get("data");

        logger.info("Webhook generated: {}", webhook);
        return new WebhookResponse(webhook, accessToken, data);
    }

    private JsonNode processData(WebhookResponse webhookResponse) {
        // Determine which question to solve based on regNo
        boolean isOdd = Integer.parseInt(REG_NO.substring(REG_NO.length() - 2)) % 2 != 0;
        
        if (isOdd) {
            logger.info("Processing Question 1: Mutual Followers");
            return solveMutualFollowers(webhookResponse.getData());
        } else {
            logger.info("Processing Question 2: Nth-Level Followers");
            return solveNthLevelFollowers(webhookResponse.getData());
        }
    }

    private JsonNode solveMutualFollowers(JsonNode data) {
        JsonNode users = data.get("users");
        List<List<Integer>> mutualFollowers = new ArrayList<>();
        Map<Integer, Set<Integer>> followMap = new HashMap<>();

        // Build follow map
        for (JsonNode user : users) {
            int userId = user.get("id").asInt();
            Set<Integer> follows = new HashSet<>();
            
            JsonNode followsArray = user.get("follows");
            for (JsonNode follow : followsArray) {
                follows.add(follow.asInt());
            }
            
            followMap.put(userId, follows);
        }

        // Find mutual followers
        for (Map.Entry<Integer, Set<Integer>> entry : followMap.entrySet()) {
            int userId = entry.getKey();
            Set<Integer> userFollows = entry.getValue();
            
            for (Integer followedId : userFollows) {
                // Skip if we've already processed this pair
                if (followedId < userId) continue;
                
                // Check if follower is followed back
                Set<Integer> followedUserFollows = followMap.get(followedId);
                if (followedUserFollows != null && followedUserFollows.contains(userId)) {
                    // Add as mutual followers
                    List<Integer> pair = Arrays.asList(userId, followedId);
                    Collections.sort(pair);  // Ensure [min, max] order
                    mutualFollowers.add(pair);
                }
            }
        }

        // Create result JSON
        ObjectNode result = objectMapper.createObjectNode();
        result.put("regNo", REG_NO);
        
        ArrayNode outcomeArray = result.putArray("outcome");
        for (List<Integer> pair : mutualFollowers) {
            ArrayNode pairNode = outcomeArray.addArray();
            pairNode.add(pair.get(0));
            pairNode.add(pair.get(1));
        }
        
        return result;
    }

    private JsonNode solveNthLevelFollowers(JsonNode data) {
        int n = data.get("n").asInt();
        int findId = data.get("findId").asInt();
        JsonNode users = data.get("users");
        
        // Build follow map
        Map<Integer, Set<Integer>> followMap = new HashMap<>();
        for (JsonNode user : users) {
            int userId = user.get("id").asInt();
            Set<Integer> follows = new HashSet<>();
            
            JsonNode followsArray = user.get("follows");
            for (JsonNode follow : followsArray) {
                follows.add(follow.asInt());
            }
            
            followMap.put(userId, follows);
        }
        
        // Find nth level followers using BFS
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> levels = new HashMap<>();
        
        queue.add(findId);
        visited.add(findId);
        levels.put(findId, 0);
        
        Set<Integer> nthLevelFollowers = new HashSet<>();
        
        while (!queue.isEmpty()) {
            int currentId = queue.poll();
            int currentLevel = levels.get(currentId);
            
            if (currentLevel == n) {
                nthLevelFollowers.add(currentId);
                continue;
            }
            
            if (currentLevel > n) {
                break;  // We've gone past the level we need
            }
            
            Set<Integer> follows = followMap.get(currentId);
            if (follows != null) {
                for (Integer followId : follows) {
                    if (!visited.contains(followId)) {
                        queue.add(followId);
                        visited.add(followId);
                        levels.put(followId, currentLevel + 1);
                    }
                }
            }
        }
        
        // Remove the starting node if it's in the results
        nthLevelFollowers.remove(findId);
        
        // Create result JSON
        ObjectNode result = objectMapper.createObjectNode();
        result.put("regNo", REG_NO);
        
        ArrayNode outcomeArray = result.putArray("outcome");
        List<Integer> sortedFollowers = new ArrayList<>(nthLevelFollowers);
        Collections.sort(sortedFollowers);
        
        for (Integer follower : sortedFollowers) {
            outcomeArray.add(follower);
        }
        
        return result;
    }

    @Retryable(maxAttempts = 4, backoff = @Backoff(delay = 1000))
    private void sendResultToWebhook(String webhookUrl, String accessToken, JsonNode result) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", accessToken);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(result), headers);
            
            logger.info("Sending result to webhook: {}", webhookUrl);
            logger.info("Result data: {}", result.toString());
            
            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            
            logger.info("Webhook response status: {}", response.getStatusCode());
            logger.info("Webhook response body: {}", response.getBody());
            
        } catch (Exception e) {
            logger.error("Error sending result to webhook (will retry): {}", e.getMessage());
            throw new RuntimeException("Failed to send result to webhook", e);
        }
    }

    private static class WebhookResponse {
        private final String webhook;
        private final String accessToken;
        private final JsonNode data;

        public WebhookResponse(String webhook, String accessToken, JsonNode data) {
            this.webhook = webhook;
            this.accessToken = accessToken;
            this.data = data;
        }

        public String getWebhook() {
            return webhook;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public JsonNode getData() {
            return data;
        }
    }
}