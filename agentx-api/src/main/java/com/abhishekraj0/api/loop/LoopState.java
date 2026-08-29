package com.abhishekraj0.api.loop;

/**
 * States representing the stages of the agentic reasoning and action loop.
 */
public enum LoopState {
    CREATED,
    INITIALIZING,
    UNDERSTANDING,
    BUILDING_CONTEXT,
    RETRIEVING_MEMORY,
    RESOLVING_TOOLS,
    PLANNING,
    DECIDING,
    VALIDATING,
    AUTHORIZING,
    WAITING_FOR_APPROVAL,
    EXECUTING,
    OBSERVING,
    UPDATING_STATE,
    EVALUATING_GOAL,
    REPLANNING,
    WAITING_FOR_USER,
    COMPLETED,
    FAILED,
    CANCELLED,
    TIMEOUT
}
