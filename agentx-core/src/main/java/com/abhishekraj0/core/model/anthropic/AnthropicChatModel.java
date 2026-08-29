package com.abhishekraj0.core.model.anthropic;

import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.tool.AgentTool;
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
 * ChatModel implementation for Anthropic Claude using JDK HttpClient and Jackson.
 */
public class AnthropicChatModel implements ChatModel {

    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnthropicChatModel(String apiKey, String modelName) {
        this(apiKey, modelName, "https://api.anthropic.com/v1");
    }

    public AnthropicChatModel(String apiKey, String modelName, String baseUrl) {
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
            rootNode.put("model", modelName);
            rootNode.put("max_tokens", 4096);

            StringBuilder systemPrompt = new StringBuilder();
            ArrayNode messagesNode = rootNode.putArray("messages");

            for (ChatMessage msg : request.messages()) {
                if (msg.role() == ChatMessageRole.SYSTEM) {
                    if (!systemPrompt.isEmpty()) {
                        systemPrompt.append("\n");
                    }
                    systemPrompt.append(msg.content());
                } else {
                    ObjectNode msgNode = messagesNode.addObject();
                    msgNode.put("role", msg.role().name().toLowerCase());
                    
                    if (msg.content() != null) {
                        msgNode.put("content", msg.content());
                    }
                }
            }

            if (!systemPrompt.isEmpty()) {
                rootNode.put("system", systemPrompt.toString());
            }

            if (request.tools() != null && !request.tools().isEmpty()) {
                ArrayNode toolsNode = rootNode.putArray("tools");
                for (AgentTool tool : request.tools()) {
                    ObjectNode toolNode = toolsNode.addObject();
                    toolNode.put("name", tool.id().name());
                    toolNode.put("description", tool.description());
                    
                    ObjectNode inputSchemaNode = toolNode.putObject("input_schema");
                    inputSchemaNode.put("type", tool.inputSchema().type());
                    ObjectNode propsNode = inputSchemaNode.putObject("properties");
                    ArrayNode reqArray = inputSchemaNode.putArray("required");
                    
                    if (tool.inputSchema().properties() != null) {
                        for (com.abhishekraj0.api.tool.ToolProperty prop : tool.inputSchema().properties()) {
                            ObjectNode propNode = propsNode.putObject(prop.name());
                            propNode.put("type", prop.type());
                            propNode.put("description", prop.description());
                            if (prop.required()) {
                                reqArray.add(prop.name());
                            }
                        }
                    }
                }
            }

            String requestBody = objectMapper.writeValueAsString(rootNode);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Anthropic API returned status code " + response.statusCode() + ": " + response.body());
            }

            ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response.body());
            ArrayNode contentArray = (ArrayNode) responseJson.get("content");
            
            String contentText = null;
            List<ToolCall> toolCalls = new ArrayList<>();

            for (int i = 0; i < contentArray.size(); i++) {
                ObjectNode item = (ObjectNode) contentArray.get(i);
                String type = item.get("type").asText();
                if ("text".equals(type)) {
                    contentText = item.get("text").asText();
                } else if ("tool_use".equals(type)) {
                    String id = item.get("id").asText();
                    String name = item.get("name").asText();
                    String inputJson = objectMapper.writeValueAsString(item.get("input"));
                    toolCalls.add(new ToolCall(id, name, inputJson));
                }
            }

            ChatMessage assistantMessage = ChatMessage.assistant(contentText, toolCalls);
            ObjectNode usageNode = (ObjectNode) responseJson.get("usage");
            int inputTokens = usageNode.get("input_tokens").asInt();
            int outputTokens = usageNode.get("output_tokens").asInt();
            TokenUsage usage = new TokenUsage(inputTokens, outputTokens, inputTokens + outputTokens);

            String stopReason = responseJson.has("stop_reason") ? responseJson.get("stop_reason").asText() : "end_turn";

            return new ChatResponse(assistantMessage, usage, stopReason);

        } catch (Exception e) {
            throw new RuntimeException("Failed calling Anthropic Chat API", e);
        }
    }

    @Override
    public ModelMetadata metadata() {
        return new ModelMetadata(modelName, "anthropic", 200000, false, true);
    }
}
