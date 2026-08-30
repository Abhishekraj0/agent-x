package com.abhishekraj0.api.loop;

import com.abhishekraj0.api.context.AgentContext;
import com.abhishekraj0.api.agent.AgentDecision;
import com.abhishekraj0.api.tool.AgentTool;
import java.util.List;

/**
 * Interface to select the next decision/action of the agent.
 */
public interface ActionSelector {
    AgentDecision select(AgentContext context, List<AgentTool> tools);

    default com.abhishekraj0.api.model.TokenUsage lastTokenUsage() {
        return com.abhishekraj0.api.model.TokenUsage.zero();
    }

    default com.abhishekraj0.api.model.ModelMetadata metadata() {
        return new com.abhishekraj0.api.model.ModelMetadata("unknown", "unknown", 8192, false, false, new com.abhishekraj0.api.model.ModelCapabilities(false, false, false, false, false));
    }
}
