package com.abhishekraj0.api.loop;

import com.abhishekraj0.api.agent.AgentState;

/**
 * Interface representing a transition between states in the agent loop.
 */
public interface LoopTransition {
    LoopState from();
    LoopState to();
    boolean canTransition(AgentState state);
    AgentState apply(AgentState state);
}
