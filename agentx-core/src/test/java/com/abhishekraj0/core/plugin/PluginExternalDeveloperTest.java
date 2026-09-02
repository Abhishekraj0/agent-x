package com.abhishekraj0.core.plugin;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.plugin.AgentPlugin;
import com.abhishekraj0.api.tool.AgentTool;
import com.abhishekraj0.api.tool.ToolContext;
import com.abhishekraj0.api.tool.ToolId;
import com.abhishekraj0.api.tool.ToolResult;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

public class PluginExternalDeveloperTest {

    @Test
    public void testCleanRoomExternalPluginJarLoadingAndExecution() throws Exception {
        // Resolve external plugin example location (prefer pre-built JAR/classes, fallback to dynamic compile)
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

        // If target classes/jar do not exist (clean CI build where core compiles before example plugins), compile dynamically
        if (!location.toFile().exists()) {
            location = compileExternalPluginDynamically();
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

    private Path compileExternalPluginDynamically() throws IOException {
        Path sourceDir = Path.of("../agentx-plugin-example/src/main/java");
        if (!Files.exists(sourceDir)) {
            sourceDir = Path.of("agentx-plugin-example/src/main/java");
        }
        Path resourceDir = Path.of("../agentx-plugin-example/src/main/resources");
        if (!Files.exists(resourceDir)) {
            resourceDir = Path.of("agentx-plugin-example/src/main/resources");
        }

        Path outDir = Path.of("target/test-external-plugin-classes");
        Files.createDirectories(outDir);

        final Path finalResourceDir = resourceDir;
        // Copy resources (including META-INF/services/com.abhishekraj0.api.plugin.AgentPlugin)
        if (Files.exists(finalResourceDir)) {
            try (Stream<Path> stream = Files.walk(finalResourceDir)) {
                stream.forEach(src -> {
                    try {
                        Path dest = outDir.resolve(finalResourceDir.relativize(src));
                        if (Files.isDirectory(src)) {
                            Files.createDirectories(dest);
                        } else {
                            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler != null && Files.exists(sourceDir)) {
            try (Stream<Path> stream = Files.walk(sourceDir)) {
                List<File> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).map(Path::toFile).toList();
                StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
                Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(javaFiles);
                String classpath = System.getProperty("java.class.path");
                List<String> options = List.of("-d", outDir.toAbsolutePath().toString(), "-classpath", classpath);
                compiler.getTask(null, fileManager, null, options, null, units).call();
                fileManager.close();
            }
        }
        return outDir;
    }
}
