package com.abhishekraj0.examples;

import com.abhishekraj0.api.agent.Agent;
import com.abhishekraj0.api.agent.AgentResponse;
import com.abhishekraj0.api.model.ChatModel;
import com.abhishekraj0.api.model.ChatMessage;
import com.abhishekraj0.api.model.ChatResponse;
import com.abhishekraj0.api.model.ToolCall;
import com.abhishekraj0.api.model.TokenUsage;
import com.abhishekraj0.api.tool.AgentTool;
import com.abhishekraj0.api.tool.ToolId;
import com.abhishekraj0.api.tool.ToolRegistry;
import com.abhishekraj0.api.tool.ToolResult;
import com.abhishekraj0.api.tool.ToolSchema;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import com.abhishekraj0.core.tool.FunctionTool;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AgentXDemoApplication {

    private static final Logger log = LoggerFactory.getLogger(AgentXDemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AgentXDemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner runner(Agent agent, ChatModel chatModel, ToolRegistry toolRegistry) {
        return args -> {
            log.info("=== Starting AgentX Demo Application ===");

            MockChatModel mockModel = (MockChatModel) chatModel;
            DefaultToolRegistry registry = (DefaultToolRegistry) toolRegistry;

            // 1. Register a custom tool
            ToolId calculatorId = new ToolId("math-calculator");
            AgentTool calculatorTool = new FunctionTool(
                    calculatorId,
                    "Performs basic arithmetic operations.",
                    ToolSchema.empty(),
                    context -> {
                        Number a = (Number) context.arguments().get("a");
                        Number b = (Number) context.arguments().get("b");
                        double result = a.doubleValue() + b.doubleValue();
                        log.info("[Tool Executed] math-calculator: {} + {} = {}", a, b, result);
                        return ToolResult.success(String.valueOf(result));
                    }
            );
            registry.register(calculatorTool);

            // 2. Configure model behaviors for this run
            mockModel.setHandler(request -> {
                boolean hasToolResponse = request.messages().stream()
                        .anyMatch(msg -> msg.role() == com.abhishekraj0.api.model.ChatMessageRole.TOOL);

                if (!hasToolResponse) {
                    // First step: model calls tool
                    ToolCall call = new ToolCall("call-demo-1", "math-calculator", "{\"a\": 15, \"b\": 25}");
                    return new ChatResponse(
                            ChatMessage.assistant(null, List.of(call)),
                            new TokenUsage(8, 12, 20),
                            "TOOL_USE"
                    );
                } else {
                    // Second step: model answers
                    return new ChatResponse(
                            ChatMessage.assistant("The sum is 40.0. Task complete."),
                            new TokenUsage(12, 6, 18),
                            "STOP"
                    );
                }
            });

            // 3. Execute request
            log.info("[User Query] Calculate 15 + 25");
            AgentResponse response = agent.run("Calculate 15 + 25");

            // 4. Print outcomes
            log.info("=== Execution Complete ===");
            log.info("Final Agent Response: {}", response.output());
            log.info("Execution Status: {}", response.state().status());
            log.info("Total Iterations: {}", response.state().iterations());
            log.info("Total Tool Calls: {}", response.state().toolCalls());
            log.info("Events Fired:");
            response.events().forEach(event -> log.info(" - Event [{}]: Id={}, Time={}", event.type(), event.id(), event.timestamp()));
        };
    }
}
