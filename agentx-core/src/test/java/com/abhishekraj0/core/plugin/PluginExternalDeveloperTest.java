package com.abhishekraj0.core.plugin;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.plugin.AgentPlugin;
import com.abhishekraj0.api.tool.AgentTool;
import com.abhishekraj0.api.tool.ToolContext;
import com.abhishekraj0.api.tool.ToolId;
import com.abhishekraj0.api.tool.ToolResult;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class PluginExternalDeveloperTest {

    @Test
    public void testCleanRoomExternalPluginJarLoadingAndExecution() throws Exception {
        // Resolve external plugin example location (prefer packaged JAR, fallback to classes dir)
        Path location = Path.of("../agentx-plugin-example/target/agentx-plugin-example-1.0.0-SNAPSHOT.jar");
        if (!location.toFile().exists()) {
            location = Path.of("agentx-plugin-example/target/agentx-plugin-example-1.0.0-SNAPSHOT.jar");
        }
        if (!location.toFile().exists()) {
            location = Path.of("../agentx-plugin-example/target/classes");
        }
        if (!location.toFile().exists()) {
            location = Path.of("agentx-plugin-example/target/classes");
        }

        assertTrue(location.toFile().exists(), "Plugin example location must exist at: " + location.toAbsolutePath());

        // Construct dynamic URLClassLoader simulating external classpath placement
        URL[] urls = new URL[]{location.toUri().toURL()};
        try (URLClassLoader jarClassLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader())) {
            
            DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
            DefaultPluginContext context = new DefaultPluginContext(
                    toolRegistry,
                    null,
                    null,
                    null,
                    Map.of("environment", "external-test")
            );

            PluginManager pluginManager = new PluginManager(context);
            pluginManager.loadPlugins(jarClassLoader);

            List<AgentPlugin> loadedPlugins = pluginManager.getActivePlugins();
            assertTrue(loadedPlugins.stream().anyMatch(p -> "calculator-plugin".equals(p.metadata().id())),
                    "calculator-plugin must be discovered from external project");
            assertTrue(loadedPlugins.stream().anyMatch(p -> "greeting-plugin".equals(p.metadata().id())),
                    "greeting-plugin must be discovered from external project");

            // Verify tools registered by external project
            AgentTool calcTool = toolRegistry.get(new ToolId("calculator", "add"))
                    .orElseThrow(() -> new AssertionError("Calculator tool not registered by external plugin"));

            AgentTool greetTool = toolRegistry.get(new ToolId("greeting", "sayHello"))
                    .orElseThrow(() -> new AssertionError("Greeting tool not registered by external plugin"));

            // Execute calculator:add tool
            ToolResult calcResult = calcTool.execute(new ToolContext("exec-1", Map.of("a", 15, "b", 27)));
            assertTrue(calcResult.success());
            assertEquals("42.0", calcResult.output());

            // Execute greeting:sayHello tool
            ToolResult greetResult = greetTool.execute(new ToolContext("exec-2", Map.of("name", "AgentX")));
            assertTrue(greetResult.success());
            assertEquals("Hello, AgentX!", greetResult.output());

            // Shutdown plugins cleanly
            pluginManager.shutdownPlugins();
            assertTrue(pluginManager.getActivePlugins().isEmpty());
        }
    }
}
