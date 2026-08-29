package com.agentx.core.loop;

import com.agentx.api.agent.*;
import com.agentx.api.context.*;
import com.agentx.api.loop.*;
import com.agentx.api.model.*;
import com.agentx.api.security.*;
import com.agentx.api.tool.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/**
 * Default execution engine that evaluates a single reasoning step by calling the LLM and executing any requested tools.
 */
public class DefaultExecutionEngine implements ExecutionEngine {

    private final ChatModel model;
    private final ToolRegistry toolRegistry;
    private final List<Guardrail> guardrails;
    private final PermissionManager permissionManager;
    private final ApprovalProvider approvalProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DefaultExecutionEngine(ChatModel model, ToolRegistry toolRegistry, List<Guardrail> guardrails, PermissionManager permissionManager, ApprovalProvider approvalProvider) {
        this.model = model;
        this.toolRegistry = toolRegistry;
        this.guardrails = guardrails != null ? guardrails : List.of();
        this.permissionManager = permissionManager;
        this.approvalProvider = approvalProvider;
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        AgentState state = request.state();
        AgentRequest agentReq = request.request();

        List<ChatMessage> messages = new ArrayList<>(state.history());
        List<AgentTool> availableTools = new ArrayList<>(toolRegistry != null ? toolRegistry.all() : List.of());

        ChatRequest chatRequest = new ChatRequest(
                messages,
                availableTools,
                agentReq.options() != null ? agentReq.options().temperature() : null,
                Map.of()
        );

        ChatResponse chatResponse;
        try {
            chatResponse = model.chat(chatRequest);
        } catch (Exception e) {
            return new ExecutionResult(state, null, false, e);
        }

        ChatMessage responseMessage = chatResponse.message();

        List<ChatMessage> updatedHistory = new ArrayList<>(state.history());
        updatedHistory.add(responseMessage);

        state = new AgentState(
                state.executionId(),
                updatedHistory,
                state.plan(),
                state.variables(),
                state.iterations(),
                state.toolCalls(),
                state.status()
        );

        if (responseMessage.toolCalls() != null && !responseMessage.toolCalls().isEmpty()) {
            int toolCallCount = state.toolCalls();
            for (ToolCall toolCall : responseMessage.toolCalls()) {
                toolCallCount++;

                Optional<AgentTool> toolOpt = toolRegistry.get(new ToolId(toolCall.name()));
                if (toolOpt.isEmpty()) {
                    String errorMsg = "Tool not found: " + toolCall.name();
                    updatedHistory.add(ChatMessage.tool(toolCall.id(), errorMsg));
                    continue;
                }

                AgentTool tool = toolOpt.get();
                AgentAction action = new AgentAction(
                        UUID.randomUUID().toString(),
                        "TOOL_CALL",
                        Map.of("toolName", tool.id().name(), "arguments", toolCall.argumentsJson())
                );

                AgentContext currentAgentContext = new AgentContext(updatedHistory, state.variables(), "", Map.of());

                // Validate Guardrails
                for (Guardrail guardrail : guardrails) {
                    GuardrailResult gr = guardrail.validate(action, currentAgentContext);
                    if (!gr.passed()) {
                        String grError = "Guardrail validation failed: " + gr.failureReason();
                        updatedHistory.add(ChatMessage.tool(toolCall.id(), grError));
                        state = new AgentState(
                                state.executionId(), updatedHistory, state.plan(), state.variables(),
                                state.iterations(), toolCallCount, state.status()
                        );
                        return new ExecutionResult(state, grError, true, null);
                    }
                }

                // Verify Permissions
                if (permissionManager != null) {
                    PermissionDecision decision = permissionManager.check(action, currentAgentContext);
                    if (decision.status() == PermissionStatus.DENY) {
                        String denyMsg = "Permission Denied: " + decision.reason();
                        updatedHistory.add(ChatMessage.tool(toolCall.id(), denyMsg));
                        state = new AgentState(
                                state.executionId(), updatedHistory, state.plan(), state.variables(),
                                state.iterations(), toolCallCount, state.status()
                        );
                        return new ExecutionResult(state, denyMsg, true, null);
                    } else if (decision.status() == PermissionStatus.REQUIRE_APPROVAL || tool.metadata().requiresApproval()) {
                        if (approvalProvider == null) {
                            String errorMsg = "Human approval required but no approval provider is configured.";
                            updatedHistory.add(ChatMessage.tool(toolCall.id(), errorMsg));
                            state = new AgentState(
                                    state.executionId(), updatedHistory, state.plan(), state.variables(),
                                    state.iterations(), toolCallCount, state.status()
                            );
                            return new ExecutionResult(state, errorMsg, true, null);
                        }

                        ApprovalRequest approvalReq = approvalProvider.request(new ApprovalContext(state.executionId(), action, currentAgentContext));
                        ApprovalResult approvalRes = approvalProvider.waitFor(approvalReq);

                        if (!approvalRes.approved()) {
                            String rejectMsg = "Approval Rejected: " + approvalRes.reason();
                            updatedHistory.add(ChatMessage.tool(toolCall.id(), rejectMsg));
                            state = new AgentState(
                                    state.executionId(), updatedHistory, state.plan(), state.variables(),
                                    state.iterations(), toolCallCount, state.status()
                            );
                            return new ExecutionResult(state, rejectMsg, true, null);
                        }
                    }
                }

                // Run Tool
                Map<String, Object> parsedArgs;
                try {
                    parsedArgs = objectMapper.readValue(toolCall.argumentsJson(), new TypeReference<>() {});
                } catch (Exception e) {
                    parsedArgs = Map.of();
                }

                ToolContext toolContext = new ToolContext(state.executionId(), parsedArgs, state.variables());
                ToolResult toolResult = tool.execute(toolContext);

                String toolOutput = toolResult.success() ? toolResult.output() : "Error: " + toolResult.error().message();
                updatedHistory.add(ChatMessage.tool(toolCall.id(), toolOutput));
            }

            state = new AgentState(
                    state.executionId(),
                    updatedHistory,
                    state.plan(),
                    state.variables(),
                    state.iterations(),
                    toolCallCount,
                    state.status()
            );

            return new ExecutionResult(state, "Executed tool calls.", true, null);
        } else {
            // Model returned a text response without calling tools -> complete the loop
            state = new AgentState(
                    state.executionId(),
                    updatedHistory,
                    state.plan(),
                    state.variables(),
                    state.iterations(),
                    state.toolCalls(),
                    "COMPLETED"
            );
            return new ExecutionResult(state, responseMessage.content(), true, null);
        }
    }
}
