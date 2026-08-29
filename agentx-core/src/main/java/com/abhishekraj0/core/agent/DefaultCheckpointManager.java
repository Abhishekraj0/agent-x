package com.abhishekraj0.core.agent;

import com.abhishekraj0.api.agent.AgentCheckpoint;
import com.abhishekraj0.api.agent.AgentState;
import com.abhishekraj0.api.agent.CheckpointManager;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Standard implementation of CheckpointManager.
 */
public class DefaultCheckpointManager implements CheckpointManager {

    @Override
    public AgentCheckpoint checkpoint(AgentState state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        return new AgentCheckpoint(
                UUID.randomUUID().toString(),
                state.executionId(),
                state,
                Instant.now(),
                Map.of()
        );
    }

    @Override
    public AgentState restore(AgentCheckpoint checkpoint) {
        if (checkpoint == null) {
            throw new IllegalArgumentException("Checkpoint cannot be null");
        }
        return checkpoint.state();
    }
}
