package com.agentx.core.agent;

import com.agentx.api.agent.Agent;
import com.agentx.api.agent.AgentResponse;
import com.agentx.api.model.ChatMessage;
import com.agentx.api.model.ChatResponse;
import com.agentx.api.model.ToolCall;
import com.agentx.api.model.TokenUsage;
import com.agentx.api.tool.AgentTool;
import com.agentx.api.tool.ToolId;
import com.agentx.api.tool.ToolResult;
import com.agentx.api.tool.ToolSchema;
import com.agentx.core.AgentX;
import com.agentx.core.model.mock.MockChatModel;
import com.agentx.core.tool.DefaultToolRegistry;
import com.agentx.core.tool.FunctionTool;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AgentLoopTest {

    @Test
    public void testAgentExecutionLoop() {
        // 1. Create Tool Registry and Register a tool
        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        ToolId toolId = new ToolId("calculator");
        
        AgentTool calculator = new FunctionTool(
                toolId,
                "calculator tool",
                ToolSchema.empty(),
                context -> {
                    Number a = (Number) context.arguments().get("a");
                    Number b = (Number) context.arguments().get("b");
                    double sum = a.doubleValue() + b.doubleValue();
                    return ToolResult.success(String.valueOf(sum));
                }
        );
        toolRegistry.register(calculator);

        // 2. Create MockChatModel with dynamic responses to drive the loop
        MockChatModel model = new MockChatModel();
        model.setHandler(request -> {
            List<ChatMessage> history = request.messages();
            
            // Check if there are tool responses in the history
            boolean hasToolResponse = history.stream()
                    .anyMatch(msg -> msg.role() == com.agentx.api.model.ChatMessageRole.TOOL);

            if (!hasToolResponse) {
                // Iteration 1: The model determines it needs to call the calculator tool
                ToolCall call = new ToolCall("call-1", "calculator", "{\"a\": 5, \"b\": 10}");
                return new ChatResponse(
                        ChatMessage.assistant(null, List.of(call)),
                        new TokenUsage(5, 10, 15),
                        "TOOL_USE"
                );
            } else {
                // Iteration 2: After the tool execution, the model generates the final answer
                ChatMessage toolMessage = history.stream()
                        .filter(msg -> msg.role() == com.agentx.api.model.ChatMessageRole.TOOL)
                        .findFirst()
                        .orElseThrow();
                
                String toolResultValue = toolMessage.content();
                return new ChatResponse(
                        ChatMessage.assistant("The final result is " + toolResultValue),
                        new TokenUsage(10, 5, 15),
                        "STOP"
                );
            }
        });

        // 3. Build the Agent
        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .build();

        // 4. Run the Agent
        AgentResponse response = agent.run("Calculate 5 + 10");

        // 5. Verify outcomes
        assertNotNull(response);
        assertEquals("The final result is 15.0", response.output());
        assertEquals("COMPLETED", response.state().status());
        assertEquals(2, response.state().iterations());
        assertEquals(1, response.state().toolCalls());
        
        // Verify message history
        List<ChatMessage> history = response.state().history();
        assertEquals(4, history.size()); // User request, Tool call, Tool result, Final answer
        assertEquals("Calculate 5 + 10", history.get(0).content());
        assertNotNull(history.get(1).toolCalls());
        assertEquals("calculator", history.get(1).toolCalls().get(0).name());
        assertEquals("15.0", history.get(2).content());
        assertEquals("The final result is 15.0", history.get(3).content());
    }
}
