package com.abhishekraj0.core.plugin;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.event.AgentEvent;
import com.abhishekraj0.api.event.EventBus;
import com.abhishekraj0.api.plugin.AgentPlugin;
import com.abhishekraj0.api.plugin.PluginContext;
import com.abhishekraj0.api.plugin.PluginMetadata;
import com.abhishekraj0.api.plugin.PluginState;
import com.abhishekraj0.api.plugin.exception.DuplicatePluginException;
import com.abhishekraj0.api.plugin.exception.PluginException;
import com.abhishekraj0.api.plugin.exception.PluginToolCollisionException;
import com.abhishekraj0.api.plugin.exception.PluginValidationException;
import com.abhishekraj0.api.tool.AgentTool;
import com.abhishekraj0.api.tool.ToolContext;
import com.abhishekraj0.api.tool.ToolId;
import com.abhishekraj0.api.tool.ToolProperty;
import com.abhishekraj0.api.tool.ToolResult;
import com.abhishekraj0.api.tool.ToolSchema;
import com.abhishekraj0.core.event.SimpleEventBus;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PluginEcosystemTest {

    private DefaultToolRegistry toolRegistry;
    private EventBus eventBus;
    private DefaultPluginContext context;
    private PluginManager pluginManager;
    private List<AgentEvent> emittedEvents;

    @BeforeEach
    public void setUp() {
        toolRegistry = new DefaultToolRegistry();
        eventBus = new SimpleEventBus();
        emittedEvents = new ArrayList<>();
        eventBus.subscribe(AgentEvent.class, emittedEvents::add);

        context = new DefaultPluginContext(
                toolRegistry,
                null,
                null,
                eventBus,
                Map.of("env", "test")
        );
        pluginManager = new PluginManager(context);
    }

    @Test
    public void testSingleValidPluginRegistration() {
        MockPlugin plugin = new MockPlugin("plugin-1", "1.0.0", "tool-1");
        pluginManager.registerPlugin(plugin);

        assertEquals(1, pluginManager.getActivePlugins().size());
        assertEquals(PluginState.ACTIVE, pluginManager.getPluginState("plugin-1"));
        assertTrue(toolRegistry.get(new ToolId("plugin-1", "tool-1")).isPresent());
        assertFalse(emittedEvents.isEmpty());
    }

    @Test
    public void testMultipleValidPluginsRegistration() {
        MockPlugin plugin1 = new MockPlugin("plugin-1", "1.0.0", "tool-1");
        MockPlugin plugin2 = new MockPlugin("plugin-2", "2.0.0", "tool-2");

        pluginManager.registerPlugin(plugin1);
        pluginManager.registerPlugin(plugin2);

        assertEquals(2, pluginManager.getActivePlugins().size());
        assertTrue(toolRegistry.get(new ToolId("plugin-1", "tool-1")).isPresent());
        assertTrue(toolRegistry.get(new ToolId("plugin-2", "tool-2")).isPresent());
    }

    @Test
    public void testDuplicatePluginIdRejection() {
        MockPlugin plugin1 = new MockPlugin("dup-plugin", "1.0.0", "tool-1");
        MockPlugin plugin2 = new MockPlugin("dup-plugin", "1.0.1", "tool-2");

        pluginManager.registerPlugin(plugin1);
        assertThrows(DuplicatePluginException.class, () -> pluginManager.registerPlugin(plugin2));
    }

    @Test
    public void testDuplicateToolCollisionRejection() {
        // Two different plugins trying to register tool with exact same full name
        MockPlugin plugin1 = new MockPlugin("plugin-a", "1.0.0", "shared-tool", "common-ns");
        MockPlugin plugin2 = new MockPlugin("plugin-b", "1.0.0", "shared-tool", "common-ns");

        pluginManager.registerPlugin(plugin1);
        assertThrows(PluginToolCollisionException.class, () -> pluginManager.registerPlugin(plugin2));
    }

    @Test
    public void testInvalidPluginMetadataRejection() {
        AgentPlugin nullIdPlugin = new AgentPlugin() {
            @Override public PluginMetadata metadata() { return new PluginMetadata(null, "Name", "1.0", "Desc", "Author"); }
            @Override public void initialize(PluginContext context) {}
            @Override public void shutdown() {}
        };
        assertThrows(PluginValidationException.class, () -> pluginManager.registerPlugin(nullIdPlugin));

        AgentPlugin blankVersionPlugin = new AgentPlugin() {
            @Override public PluginMetadata metadata() { return new PluginMetadata("valid-id", "Name", "   ", "Desc", "Author"); }
            @Override public void initialize(PluginContext context) {}
            @Override public void shutdown() {}
        };
        assertThrows(PluginValidationException.class, () -> pluginManager.registerPlugin(blankVersionPlugin));
    }

    @Test
    public void testPluginInitializationFailureHandling() {
        AgentPlugin failingPlugin = new AgentPlugin() {
            @Override public PluginMetadata metadata() { return new PluginMetadata("failing-plugin", "Failing", "1.0.0", "Desc", "Author"); }
            @Override public void initialize(PluginContext context) {
                throw new IllegalStateException("Database connection failed");
            }
            @Override public void shutdown() {}
        };

        assertThrows(PluginException.class, () -> pluginManager.registerPlugin(failingPlugin));
        assertEquals(PluginState.FAILED, pluginManager.getPluginState("failing-plugin"));
    }

    @Test
    public void testPluginShutdownFailureResilience() {
        MockPlugin plugin1 = new MockPlugin("plugin-normal", "1.0.0", "tool-1");
        AgentPlugin failingShutdownPlugin = new AgentPlugin() {
            @Override public PluginMetadata metadata() { return new PluginMetadata("plugin-error-shutdown", "ErrorShutdown", "1.0.0", "Desc", "Author"); }
            @Override public void initialize(PluginContext context) {}
            @Override public void shutdown() {
                throw new RuntimeException("Error during shutdown cleanup");
            }
        };

        pluginManager.registerPlugin(plugin1);
        pluginManager.registerPlugin(failingShutdownPlugin);

        // Shutdown should complete without aborting remaining plugins
        assertDoesNotThrow(() -> pluginManager.shutdownPlugins());
        assertTrue(plugin1.isShutdown());
        assertEquals(PluginState.FAILED, pluginManager.getPluginState("plugin-error-shutdown"));
        assertEquals(PluginState.SHUTDOWN, pluginManager.getPluginState("plugin-normal"));
    }

    @Test
    public void testPluginToolExecutionParticipation() {
        MockPlugin plugin = new MockPlugin("plugin-exec", "1.0.0", "add");
        pluginManager.registerPlugin(plugin);

        AgentTool tool = toolRegistry.get(new ToolId("plugin-exec", "add")).orElseThrow();
        ToolResult result = tool.execute(new ToolContext("exec-1", Map.of("x", 10, "y", 20)));

        assertTrue(result.success());
        assertEquals("30.0", result.output());
    }

    // Helper Mock Plugin class
    private static class MockPlugin implements AgentPlugin {
        private final PluginMetadata metadata;
        private final String toolName;
        private final String toolNamespace;
        private boolean shutdown = false;

        public MockPlugin(String id, String version, String toolName) {
            this(id, version, toolName, id);
        }

        public MockPlugin(String id, String version, String toolName, String toolNamespace) {
            this.metadata = new PluginMetadata(id, id, version, "Mock Description", "Mock Author");
            this.toolName = toolName;
            this.toolNamespace = toolNamespace;
        }

        @Override
        public PluginMetadata metadata() {
            return metadata;
        }

        @Override
        public void initialize(PluginContext context) {
            context.tools().register(new AgentTool() {
                @Override public ToolId id() { return new ToolId(toolNamespace, toolName); }
                @Override public String description() { return "Mock Tool"; }
                @Override public ToolSchema inputSchema() { return new ToolSchema("object", List.of()); }
                @Override public ToolResult execute(ToolContext ctx) {
                    double x = Double.parseDouble(String.valueOf(ctx.arguments().getOrDefault("x", 0)));
                    double y = Double.parseDouble(String.valueOf(ctx.arguments().getOrDefault("y", 0)));
                    return ToolResult.success(String.valueOf(x + y));
                }
            });
        }

        @Override
        public void shutdown() {
            this.shutdown = true;
        }

        public boolean isShutdown() {
            return shutdown;
        }
    }
}
