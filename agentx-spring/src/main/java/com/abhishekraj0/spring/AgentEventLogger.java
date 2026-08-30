package com.abhishekraj0.spring;

import com.abhishekraj0.api.event.EventBus;
import com.abhishekraj0.core.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Automatically subscribes to AgentX execution events and logs execution state.
 */
public class AgentEventLogger implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AgentEventLogger.class);
    private final EventBus eventBus;

    public AgentEventLogger(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void afterPropertiesSet() {
        if (eventBus == null) return;

        eventBus.subscribe(AgentStartedEvent.class, e ->
                log.info("[AgentX] Execution ID: {} - Agent execution started.", e.executionId())
        );

        eventBus.subscribe(AgentCompletedEvent.class, e ->
                log.info("[AgentX] Execution ID: {} - Agent completed successfully. Output length: {}", e.executionId(), e.output() != null ? e.output().length() : 0)
        );

        eventBus.subscribe(AgentFailedEvent.class, e ->
                log.error("[AgentX] Execution ID: {} - Agent execution failed. Error: {}", e.executionId(), e.error() != null ? e.error().getMessage() : "Unknown error")
        );

        eventBus.subscribe(AgentCancelledEvent.class, e ->
                log.warn("[AgentX] Execution ID: {} - Agent execution cancelled. Reason: {}", e.executionId(), e.reason())
        );

        eventBus.subscribe(StateTransitionEvent.class, e ->
                log.debug("[AgentX] Execution ID: {} - Transitioned from {} to {}", e.executionId(), e.fromState(), e.toState())
        );

        eventBus.subscribe(ToolCalledEvent.class, e ->
                log.info("[AgentX] Execution ID: {} - Tool called: {} with arguments: {}. Output length: {}", e.executionId(), e.toolName(), e.argumentsJson(), e.output() != null ? e.output().length() : 0)
        );

        eventBus.subscribe(PlanCreatedEvent.class, e ->
                log.info("[AgentX] Execution ID: {} - Plan created. Has steps: {}", e.executionId(), e.plan() != null && e.plan().steps() != null ? e.plan().steps().size() : 0)
        );
    }
}
