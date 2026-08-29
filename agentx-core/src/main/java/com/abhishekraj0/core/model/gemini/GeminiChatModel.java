package com.abhishekraj0.core.model.gemini;

import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.tool.AgentTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatModel implementation for Google Gemini using JDK HttpClient and Jackson.
 */
public class GeminiChatModel implements ChatModel {

    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiChatModel(String apiKey, String modelName) {
        this(apiKey, modelName, "https://generativelanguage.googleapis.com/v1beta");
    }

    public GeminiChatModel(String apiKey, String modelName, String baseUrl) {
        this.apiKey = apiKey;
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
            
            ArrayNode contentsNode = rootNode.putArray("contents");
            StringBuilder systemPrompt = new StringBuilder();

            for (ChatMessage msg : request.messages()) {
                if (msg.role() == ChatMessageRole.SYSTEM) {
                    if (!systemPrompt.isEmpty()) {
                        systemPrompt.append("\n");
                    }
                    systemPrompt.append(msg.content());
                } else {
                    ObjectNode contentNode = contentsNode.addObject();
                    contentNode.put("role", msg.role() == ChatMessageRole.ASSISTANT ? "model" : "user");
                    ArrayNode partsNode = contentNode.putArray("parts");
                    ObjectNode partNode = partsNode.addObject();
                    partNode.put("text", msg.content());
                }
            }

            if (!systemPrompt.isEmpty()) {
                ObjectNode sysInstructionNode = rootNode.putObject("systemInstruction");
                ArrayNode sysPartsNode = sysInstructionNode.putArray("parts");
                ObjectNode sysPartNode = sysPartsNode.addObject();
                sysPartNode.put("text", systemPrompt.toString());
            }

            if (request.tools() != null && !request.tools().isEmpty()) {
                ArrayNode toolsNode = rootNode.putArray("tools");
                ObjectNode functionDeclarationsWrapper = toolsNode.addObject();
                ArrayNode funcDeclarations = functionDeclarationsWrapper.putArray("functionDeclarations");
                
                for (AgentTool tool : request.tools()) {
                    ObjectNode funcDecl = funcDeclarations.addObject();
                    funcDecl.put("name", tool.id().name());
                    funcDecl.put("description", tool.description());
                    
                    ObjectNode parametersNode = funcDecl.putObject("parameters");
                    parametersNode.put("type", tool.inputSchema().type().toUpperCase());
                    ObjectNode propsNode = parametersNode.putObject("properties");
                    ArrayNode reqArray = parametersNode.putArray("required");
                    
                    if (tool.inputSchema().properties() != null) {
                        for (com.abhishekraj0.api.tool.ToolProperty prop : tool.inputSchema().properties()) {
                            ObjectNode propNode = propsNode.putObject(prop.name());
                            propNode.put("type", prop.type().toUpperCase());
                            propNode.put("description", prop.description());
                            if (prop.required()) {
                                reqArray.add(prop.name());
                            }
                        }
                    }
                }
            }

            if (request.temperature() != null) {
                ObjectNode genConfig = rootNode.putObject("generationConfig");
                genConfig.put("temperature", request.temperature());
            }

            String requestBody = objectMapper.writeValueAsString(rootNode);
            String url = baseUrl + "/models/" + modelName + ":generateContent?key=" + apiKey;

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Gemini API returned status code " + response.statusCode() + ": " + response.body());
            }

            ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response.body());
            JsonNode candidates = responseJson.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new RuntimeException("Gemini API returned no candidates: " + response.body());
            }
            
            ObjectNode candidate = (ObjectNode) candidates.get(0);
            ObjectNode content = (ObjectNode) candidate.get("content");
            ArrayNode parts = (ArrayNode) content.get("parts");

            String contentText = null;
            List<ToolCall> toolCalls = new ArrayList<>();

            for (int i = 0; i < parts.size(); i++) {
                ObjectNode part = (ObjectNode) parts.get(i);
                if (part.has("text")) {
                    contentText = part.get("text").asText();
                } else if (part.has("functionCall")) {
                    ObjectNode fc = (ObjectNode) part.get("functionCall");
                    String name = fc.get("name").asText();
                    String argsJson = objectMapper.writeValueAsString(fc.get("args"));
                    toolCalls.add(new ToolCall("gemini_call_" + System.nanoTime(), name, argsJson));
                }
            }

            ChatMessage assistantMessage = ChatMessage.assistant(contentText, toolCalls);
            
            TokenUsage usage = new TokenUsage(0, 0, 0);
            if (responseJson.has("usageMetadata")) {
                ObjectNode usageNode = (ObjectNode) responseJson.get("usageMetadata");
                int inputTokens = usageNode.path("promptTokenCount").asInt();
                int outputTokens = usageNode.path("candidatesTokenCount").asInt();
                int totalTokens = usageNode.path("totalTokenCount").asInt();
                usage = new TokenUsage(inputTokens, outputTokens, totalTokens);
            }

            String finishReason = candidate.path("finishReason").asText("STOP");

            return new ChatResponse(assistantMessage, usage, finishReason);

        } catch (Exception e) {
            throw new RuntimeException("Failed calling Gemini Chat API", e);
        }
    }

    @Override
    public ModelMetadata metadata() {
        return new ModelMetadata(modelName, "gemini", 1000000, false, true);
    }
}
