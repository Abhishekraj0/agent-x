package com.agentx.core;

import com.agentx.api.agent.Agent;
import com.agentx.api.context.ContextManager;
import com.agentx.api.loop.RetryStrategy;
import com.agentx.api.memory.MemoryStore;
import com.agentx.api.model.ChatModel;
import com.agentx.api.planner.Planner;
import com.agentx.api.security.ApprovalProvider;
import com.agentx.api.security.Guardrail;
import com.agentx.api.security.PermissionManager;
import com.agentx.api.tool.ToolRegistry;
import com.agentx.core.agent.DefaultAgent;
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

        public ChatModel getModel() { return model; }
        public Planner getPlanner() { return planner; }
        public MemoryStore getMemory() { return memory; }
        public ToolRegistry getTools() { return tools; }
        public List<Guardrail> getGuardrails() { return guardrails; }
        public PermissionManager getPermissionManager() { return permissionManager; }
        public ApprovalProvider getApprovalProvider() { return approvalProvider; }
        public RetryStrategy getRetryStrategy() { return retryStrategy; }
        public ContextManager getContextManager() { return contextManager; }

        public Agent build() {
            return new DefaultAgent(this);
        }
    }
}
