package com.abhishekraj0.core.loop;

import com.abhishekraj0.api.agent.AgentState;
import com.abhishekraj0.api.loop.AgentObservation;
import com.abhishekraj0.api.loop.ObservationHandler;
import com.abhishekraj0.api.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of ObservationHandler appending tool outputs to execution history.
 */
public class DefaultObservationHandler implements ObservationHandler {

    @Override
    public AgentState handle(AgentObservation observation, AgentState state) {
        List<ChatMessage> history = new ArrayList<>(state.history());
        
        String outputContent = observation.success() 
                ? observation.output() 
                : "Error (" + (observation.error() != null ? observation.error().getMessage() : "unknown") + "): " + observation.output();
        
        history.add(ChatMessage.tool(observation.observationId(), outputContent));

        return new AgentState(
                state.executionId(),
                history,
                state.plan(),
                state.variables(),
                state.iterations(),
                state.toolCalls() + 1,
                state.status()
        );
    }
}
