package com.abhishekraj0.core.loop;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.context.AgentContext;
import com.abhishekraj0.api.loop.ActionSelector;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.tool.AgentTool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default ActionSelector that invokes the ChatModel and maps its response to AgentDecision.
 */
public class DefaultActionSelector implements ActionSelector {

    private final ChatModel model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DefaultActionSelector(ChatModel model) {
        this.model = model;
    }

    @Override
    public AgentDecision select(AgentContext context, List<AgentTool> tools) {
        ChatRequest chatRequest = new ChatRequest(
                context.messages(),
                tools,
                null,
                Map.of()
        );

        ChatResponse response = model.chat(chatRequest);
        ChatMessage msg = response.message();
        String decisionId = UUID.randomUUID().toString();

        if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
            List<ToolCall> calls = msg.toolCalls();
            for (ToolCall call : calls) {
                if ("delegate".equalsIgnoreCase(call.name())) {
                    try {
                        Map<String, Object> args = objectMapper.readValue(call.argumentsJson(), new TypeReference<>() {});
                        String agentId = (String) args.get("agentId");
                        String subtask = (String) args.get("task");
                        if (agentId != null && subtask != null) {
                            return new DelegateDecision(decisionId, "Delegation requested via tool call", agentId, subtask);
                        }
                    } catch (Exception ignored) {}
                }
            }
            return new ToolCallDecision(decisionId, "LLM requested tool calls", calls);
        }

        return new FinalResponseDecision(decisionId, "LLM returned final text output", msg.content());
    }
}
