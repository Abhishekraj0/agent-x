package com.abhishekraj0.api.agent;

/**
 * Manages the checkpointing and restoration of AgentState at boundary limits.
 */
public interface CheckpointManager {

    AgentCheckpoint checkpoint(AgentState state);

    AgentState restore(AgentCheckpoint checkpoint);
}
