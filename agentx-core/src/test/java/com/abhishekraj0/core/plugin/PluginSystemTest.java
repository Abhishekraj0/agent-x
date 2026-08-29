package com.abhishekraj0.core.plugin;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.agent.AgentRegistry;
import com.abhishekraj0.api.plugin.AgentPlugin;
import com.abhishekraj0.api.tool.ToolRegistry;
import com.abhishekraj0.core.agent.DefaultAgentRegistry;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Validates the loader and configuration propagation inside the plugin system.
 */
public class PluginSystemTest {

    @Test
    public void testPluginLifecycle() {
        ToolRegistry toolRegistry = new DefaultToolRegistry();
        AgentRegistry agentRegistry = new DefaultAgentRegistry();
        
        DefaultPluginContext context = new DefaultPluginContext(
                toolRegistry,
                agentRegistry,
                null,
                null,
                Map.of("key", "val")
        );

        PluginManager pm = new PluginManager(context);
        pm.loadPlugins();

        assertFalse(pm.getActivePlugins().isEmpty(), "No plugins loaded via ServiceLoader");
        AgentPlugin firstPlugin = pm.getActivePlugins().get(0);
        assertTrue(firstPlugin instanceof TestPlugin);

        TestPlugin testPlugin = (TestPlugin) firstPlugin;
        assertTrue(testPlugin.isInitialized());
        assertEquals("test-plugin", testPlugin.metadata().id());

        pm.shutdownPlugins();
        assertTrue(testPlugin.isShutdown());
        assertTrue(pm.getActivePlugins().isEmpty());
    }
}
