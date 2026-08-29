package com.abhishekraj0.api.loop;

/**
 * Status of the agent's progress towards fulfilling the user's goal.
 */
public enum GoalStatus {
    COMPLETE,
    IN_PROGRESS,
    BLOCKED,
    FAILED,
    NEEDS_USER,
    TIMEOUT
}
