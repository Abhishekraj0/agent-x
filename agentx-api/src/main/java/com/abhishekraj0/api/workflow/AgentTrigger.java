package com.abhishekraj0.api.workflow;

import com.abhishekraj0.api.agent.AgentTask;

/**
 * Interface representing a trigger that initiates tasks based on events.
 */
public interface AgentTrigger {

    /**
     * Checks if the event details match the trigger criteria.
     *
     * @param context the event context
     * @return true if matches, false otherwise
     */
    boolean matches(TriggerContext context);

    /**
     * Creates an agent task corresponding to the triggered event.
     *
     * @param context the event context
     * @return the created task
     */
    AgentTask createTask(TriggerContext context);
}
