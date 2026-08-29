package com.abhishekraj0.api;

import com.abhishekraj0.api.agent.AgentOptions;
import com.abhishekraj0.api.agent.AgentRequest;
import com.abhishekraj0.api.agent.AgentState;
import com.abhishekraj0.api.model.ChatMessage;
import com.abhishekraj0.api.model.ChatMessageRole;
import com.abhishekraj0.api.tool.ToolId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ApiTest {

    @Test
    public void testAgentRequestCreation() {
        AgentRequest request = new AgentRequest("Hello, AgentX!");
        assertNotNull(request.executionId());
        assertEquals("Hello, AgentX!", request.input());
        assertNotNull(request.options());
        assertEquals(10, request.options().maxIterations());
    }

    @Test
    public void testChatMessageCreation() {
        ChatMessage msg = ChatMessage.user("Hello");
        assertEquals(ChatMessageRole.USER, msg.role());
        assertEquals("Hello", msg.content());
        assertNull(msg.toolCallId());
        assertNull(msg.toolCalls());
    }

    @Test
    public void testToolIdFullName() {
        ToolId toolIdNamespace = new ToolId("math", "add");
        assertEquals("math:add", toolIdNamespace.getFullName());

        ToolId toolIdNoNamespace = new ToolId("calculate");
        assertEquals("calculate", toolIdNoNamespace.getFullName());
    }

    @Test
    public void testAgentStateInitial() {
        AgentState state = AgentState.initial("12345");
        assertEquals("12345", state.executionId());
        assertEquals("INITIALIZED", state.status());
        assertTrue(state.history().isEmpty());
    }
}
