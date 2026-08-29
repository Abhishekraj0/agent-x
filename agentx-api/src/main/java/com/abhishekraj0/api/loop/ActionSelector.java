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
}
