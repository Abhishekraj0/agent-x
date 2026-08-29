package com.abhishekraj0.api.agent;

/**
 * Request details for triggering an agent run.
 */
public record AgentRequest(
        String input,
        String executionId,
        AgentOptions options
) {
    /**
     * Creates an AgentRequest with default options and a random execution ID.
     *
     * @param input the input string
     */
    public AgentRequest(String input) {
        this(input, java.util.UUID.randomUUID().toString(), AgentOptions.defaultOptions());
    }
}
