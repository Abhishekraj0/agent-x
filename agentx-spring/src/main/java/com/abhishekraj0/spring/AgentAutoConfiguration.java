package com.abhishekraj0.spring;

import com.abhishekraj0.api.agent.Agent;
import com.abhishekraj0.api.context.ContextManager;
import com.abhishekraj0.api.model.ChatModel;
import com.abhishekraj0.api.tool.ToolRegistry;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.context.SimpleContextManager;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration defining beans for AgentX execution components.
 */
@AutoConfiguration
@EnableConfigurationProperties(AgentProperties.class)
public class AgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ChatModel chatModel(AgentProperties properties) {
        return new MockChatModel(properties.getModelId(), properties.getModelProvider());
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolRegistry toolRegistry() {
        return new DefaultToolRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ContextManager contextManager() {
        return new SimpleContextManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public com.abhishekraj0.api.event.EventBus eventBus() {
        return new com.abhishekraj0.core.event.SimpleEventBus();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentEventLogger agentEventLogger(com.abhishekraj0.api.event.EventBus eventBus) {
        return new AgentEventLogger(eventBus);
    }

    @Bean
    @ConditionalOnMissingBean
    public Agent agent(
            ChatModel chatModel,
            ToolRegistry toolRegistry,
            ContextManager contextManager,
            org.springframework.beans.factory.ObjectProvider<com.abhishekraj0.api.event.EventBus> eventBus,
            org.springframework.beans.factory.ObjectProvider<com.abhishekraj0.api.security.Guardrail> guardrails,
            org.springframework.beans.factory.ObjectProvider<com.abhishekraj0.api.security.PermissionManager> permissionManager,
            org.springframework.beans.factory.ObjectProvider<com.abhishekraj0.api.security.ApprovalProvider> approvalProvider,
            org.springframework.beans.factory.ObjectProvider<com.abhishekraj0.api.memory.MemoryStore> memoryStore,
            org.springframework.beans.factory.ObjectProvider<com.abhishekraj0.api.planner.Planner> planner,
            org.springframework.beans.factory.ObjectProvider<com.abhishekraj0.api.loop.RetryStrategy> retryStrategy
    ) {
        var builder = AgentX.builder()
                .model(chatModel)
                .tools(toolRegistry)
                .contextManager(contextManager);

        eventBus.ifAvailable(builder::eventBus);
        guardrails.orderedStream().forEach(builder::guardrail);
        permissionManager.ifAvailable(builder::permissionManager);
        approvalProvider.ifAvailable(builder::approvalProvider);
        memoryStore.ifAvailable(builder::memory);
        planner.ifAvailable(builder::planner);
        retryStrategy.ifAvailable(builder::retryStrategy);

        return builder.build();
    }
}
