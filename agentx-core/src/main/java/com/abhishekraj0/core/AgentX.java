package com.abhishekraj0.core;

import com.abhishekraj0.api.agent.Agent;
import com.abhishekraj0.api.context.ContextManager;
import com.abhishekraj0.api.loop.RetryStrategy;
import com.abhishekraj0.api.memory.MemoryStore;
import com.abhishekraj0.api.model.ChatModel;
import com.abhishekraj0.api.planner.Planner;
import com.abhishekraj0.api.security.ApprovalProvider;
import com.abhishekraj0.api.security.Guardrail;
import com.abhishekraj0.api.security.PermissionManager;
import com.abhishekraj0.api.tool.ToolRegistry;
import com.abhishekraj0.api.event.EventBus;
import com.abhishekraj0.core.agent.DefaultAgent;
import java.util.ArrayList;
import java.util.List;

/**
 * Main builder and entry point for AgentX framework.
 */
public class AgentX {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ChatModel model;
        private Planner planner;
        private MemoryStore memory;
        private ToolRegistry tools;
        private List<Guardrail> guardrails = new ArrayList<>();
        private PermissionManager permissionManager;
        private ApprovalProvider approvalProvider;
        private RetryStrategy retryStrategy;
        private ContextManager contextManager;
        private EventBus eventBus;
        private com.abhishekraj0.api.loop.GoalEvaluator goalEvaluator;

        public Builder goalEvaluator(com.abhishekraj0.api.loop.GoalEvaluator goalEvaluator) {
            this.goalEvaluator = goalEvaluator;
            return this;
        }

        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        public Builder planner(Planner planner) {
            this.planner = planner;
            return this;
        }

        public Builder memory(MemoryStore memory) {
            this.memory = memory;
            return this;
        }

        public Builder tools(ToolRegistry tools) {
            this.tools = tools;
            return this;
        }

        public Builder guardrails(List<Guardrail> guardrails) {
            this.guardrails = guardrails;
            return this;
        }

        public Builder guardrail(Guardrail guardrail) {
            this.guardrails.add(guardrail);
            return this;
        }

        public Builder permissionManager(PermissionManager permissionManager) {
            this.permissionManager = permissionManager;
            return this;
        }

        public Builder approvalProvider(ApprovalProvider approvalProvider) {
            this.approvalProvider = approvalProvider;
            return this;
        }

        public Builder retryStrategy(RetryStrategy retryStrategy) {
            this.retryStrategy = retryStrategy;
            return this;
        }

        public Builder contextManager(ContextManager contextManager) {
            this.contextManager = contextManager;
            return this;
        }

        public Builder eventBus(EventBus eventBus) {
            this.eventBus = eventBus;
            return this;
        }

        public ChatModel getModel() { return model; }
        public Planner getPlanner() { return planner; }
        public MemoryStore getMemory() { return memory; }
        public ToolRegistry getTools() { return tools; }
        public List<Guardrail> getGuardrails() { return guardrails; }
        public PermissionManager getPermissionManager() { return permissionManager; }
        public ApprovalProvider getApprovalProvider() { return approvalProvider; }
        public RetryStrategy getRetryStrategy() { return retryStrategy; }
        public ContextManager getContextManager() { return contextManager; }
        public EventBus getEventBus() { return eventBus; }
        public com.abhishekraj0.api.loop.GoalEvaluator getGoalEvaluator() { return goalEvaluator; }

        public Agent build() {
            return new DefaultAgent(this);
        }
    }
}
