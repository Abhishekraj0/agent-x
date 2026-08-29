package com.abhishekraj0.spring;

import com.abhishekraj0.api.agent.Agent;
import com.abhishekraj0.api.context.ContextManager;
import com.abhishekraj0.api.model.ChatModel;
import com.abhishekraj0.api.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class AgentAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgentAutoConfiguration.class));

    @Test
    public void testAutoConfigurationBeansRegistered() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ChatModel.class);
            assertThat(context).hasSingleBean(ToolRegistry.class);
            assertThat(context).hasSingleBean(ContextManager.class);
            assertThat(context).hasSingleBean(Agent.class);

            Agent agent = context.getBean(Agent.class);
            assertThat(agent).isNotNull();
        });
    }

    @Test
    public void testCustomPropertiesAreMapped() {
        contextRunner.withPropertyValues("agentx.model-id=custom-model", "agentx.model-provider=custom-provider")
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentProperties.class);
                    AgentProperties props = context.getBean(AgentProperties.class);
                    assertThat(props.getModelId()).isEqualTo("custom-model");
                    assertThat(props.getModelProvider()).isEqualTo("custom-provider");
                });
    }
}
