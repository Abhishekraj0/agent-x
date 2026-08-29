package com.abhishekraj0.core.context;

import com.abhishekraj0.api.agent.AgentRequest;
import com.abhishekraj0.api.agent.AgentState;
import com.abhishekraj0.api.context.AgentContext;
import com.abhishekraj0.api.context.ContextManager;
import com.abhishekraj0.api.model.ChatMessage;
import com.abhishekraj0.api.model.ChatMessageRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ContextManager that preserves system prompts and truncates older messages 
 * when the history length exceeds the specified window size.
 */
public class SlidingWindowContextManager implements ContextManager {

    private final int maxMessages;

    public SlidingWindowContextManager(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    @Override
    public AgentContext buildContext(AgentRequest request, AgentState state) {
        AgentContext rawContext = new AgentContext(
                new ArrayList<>(state.history()),
                state.variables(),
                "You are an autonomous AI agent.",
                Map.of()
        );
        return compress(rawContext);
    }

    @Override
    public AgentContext compress(AgentContext context) {
        List<ChatMessage> history = context.messages();
        if (history.size() <= maxMessages) {
            return context;
        }

        List<ChatMessage> compressedHistory = new ArrayList<>();
        boolean hasSystem = !history.isEmpty() && history.get(0).role() == ChatMessageRole.SYSTEM;

        int startIndex;
        if (hasSystem) {
            compressedHistory.add(history.get(0));
            startIndex = history.size() - (maxMessages - 1);
            if (startIndex <= 0) {
                startIndex = 1;
            }
        } else {
            startIndex = history.size() - maxMessages;
        }

        for (int i = startIndex; i < history.size(); i++) {
            compressedHistory.add(history.get(i));
        }

        return new AgentContext(
                compressedHistory,
                context.variables(),
                context.systemInstruction(),
                context.metadata()
        );
    }
}
