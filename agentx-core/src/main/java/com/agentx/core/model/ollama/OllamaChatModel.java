package com.agentx.core.model.ollama;

import com.agentx.api.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * ChatModel implementation for Ollama using JDK HttpClient and Jackson.
 */
public class OllamaChatModel implements ChatModel {

    private final String modelName;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaChatModel(String modelName) {
        this(modelName, "http://localhost:11434");
    }

    public OllamaChatModel(String modelName, String baseUrl) {
        this.modelName = modelName;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("model", modelName);
            rootNode.put("stream", false);

            ArrayNode messagesNode = rootNode.putArray("messages");
            for (ChatMessage msg : request.messages()) {
                ObjectNode msgNode = messagesNode.addObject();
                msgNode.put("role", msg.role().name().toLowerCase());
                if (msg.content() != null) {
                    msgNode.put("content", msg.content());
                }
            }

            String requestBody = objectMapper.writeValueAsString(rootNode);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Ollama API returned status code " + response.statusCode() + ": " + response.body());
            }

            ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response.body());
            ObjectNode messageNode = (ObjectNode) responseJson.get("message");
            String content = messageNode.has("content") ? messageNode.get("content").asText() : "";

            ChatMessage assistantMessage = ChatMessage.assistant(content);
            TokenUsage usage = new TokenUsage(0, 0, 0);

            return new ChatResponse(assistantMessage, usage, "stop");

        } catch (Exception e) {
            throw new RuntimeException("Failed calling Ollama Chat API", e);
        }
    }

    @Override
    public ModelMetadata metadata() {
        return new ModelMetadata(modelName, "ollama", 8192, false, false);
    }
}
