package com.agentx.core.model.openai;

import com.agentx.api.model.*;
import com.agentx.api.tool.AgentTool;
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
import java.util.Map;

/**
 * ChatModel implementation for OpenAI using JDK HttpClient and Jackson.
 */
public class OpenAIChatModel implements ChatModel {

    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAIChatModel(String apiKey, String modelName) {
        this(apiKey, modelName, "https://api.openai.com/v1");
    }

    public OpenAIChatModel(String apiKey, String modelName, String baseUrl) {
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
            
            ArrayNode messagesNode = rootNode.putArray("messages");
            for (ChatMessage msg : request.messages()) {
                ObjectNode msgNode = messagesNode.addObject();
                msgNode.put("role", msg.role().name().toLowerCase());
                if (msg.content() != null) {
                    msgNode.put("content", msg.content());
                }
                if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                    ArrayNode tcNode = msgNode.putArray("tool_calls");
                    for (ToolCall tc : msg.toolCalls()) {
                        ObjectNode singleTc = tcNode.addObject();
                        singleTc.put("id", tc.id());
                        singleTc.put("type", "function");
                        ObjectNode funcNode = singleTc.putObject("function");
                        funcNode.put("name", tc.name());
                        funcNode.put("arguments", tc.argumentsJson());
                    }
                }
            }

            if (request.tools() != null && !request.tools().isEmpty()) {
                ArrayNode toolsNode = rootNode.putArray("tools");
                for (AgentTool tool : request.tools()) {
                    ObjectNode toolNode = toolsNode.addObject();
                    toolNode.put("type", "function");
                    ObjectNode funcNode = toolNode.putObject("function");
                    funcNode.put("name", tool.id().name());
                    funcNode.put("description", tool.description());
                    
                    ObjectNode paramsNode = funcNode.putObject("parameters");
                    paramsNode.put("type", tool.inputSchema().type());
                    ObjectNode propsNode = paramsNode.putObject("properties");
                    ArrayNode reqArray = paramsNode.putArray("required");
                    
                    if (tool.inputSchema().properties() != null) {
                        for (com.agentx.api.tool.ToolProperty prop : tool.inputSchema().properties()) {
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

            if (request.temperature() != null) {
                rootNode.put("temperature", request.temperature());
            }

            String requestBody = objectMapper.writeValueAsString(rootNode);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("OpenAI API returned status code " + response.statusCode() + ": " + response.body());
            }

            ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response.body());
            ObjectNode choice = (ObjectNode) responseJson.get("choices").get(0);
            ObjectNode messageNode = (ObjectNode) choice.get("message");
            String content = messageNode.has("content") && !messageNode.get("content").isNull() ? messageNode.get("content").asText() : null;

            List<ToolCall> toolCalls = new ArrayList<>();
            if (messageNode.has("tool_calls")) {
                ArrayNode tcArray = (ArrayNode) messageNode.get("tool_calls");
                for (int i = 0; i < tcArray.size(); i++) {
                    ObjectNode tc = (ObjectNode) tcArray.get(i);
                    String id = tc.get("id").asText();
                    ObjectNode func = (ObjectNode) tc.get("function");
                    String name = func.get("name").asText();
                    String arguments = func.get("arguments").asText();
                    toolCalls.add(new ToolCall(id, name, arguments));
                }
            }

            ChatMessage assistantMessage = ChatMessage.assistant(content, toolCalls);
            ObjectNode usageNode = (ObjectNode) responseJson.get("usage");
            TokenUsage usage = new TokenUsage(
                    usageNode.get("prompt_tokens").asInt(),
                    usageNode.get("completion_tokens").asInt(),
                    usageNode.get("total_tokens").asInt()
            );

            String finishReason = choice.get("finish_reason").asText();

            return new ChatResponse(assistantMessage, usage, finishReason);

        } catch (Exception e) {
            throw new RuntimeException("Failed calling OpenAI Chat API", e);
        }
    }

    @Override
    public ModelMetadata metadata() {
        return new ModelMetadata(modelName, "openai", 128000, false, true);
    }
}
