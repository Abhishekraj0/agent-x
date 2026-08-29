package com.abhishekraj0.api.loop;

import com.abhishekraj0.api.agent.AgentRequest;
import com.abhishekraj0.api.agent.AgentState;

/**
 * Coordinates and executes the agent loop state machine.
 */
public interface LoopController {
    LoopResult run(AgentRequest request, AgentState state);
}
