package com.abhishekraj0.core.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class PluginArchitectureTest {

    @Test
    public void testCoreDoesNotContainHardcodedPluginClasses() throws Exception {
        Path coreSourceDir = Path.of("src/main/java/com/abhishekraj0/core");
        if (!Files.exists(coreSourceDir)) {
            coreSourceDir = Path.of("agentx-core/src/main/java/com/abhishekraj0/core");
        }

        assertTrue(Files.exists(coreSourceDir), "agentx-core source directory must exist");

        try (Stream<Path> paths = Files.walk(coreSourceDir)) {
            List<Path> javaFiles = paths.filter(p -> p.toString().endsWith(".java")).toList();
            for (Path javaFile : javaFiles) {
                String content = Files.readString(javaFile);
                assertFalse(content.contains("CalculatorPlugin"),
                        "Core file [" + javaFile.getFileName() + "] contains hardcoded plugin reference: CalculatorPlugin");
                assertFalse(content.contains("GreetingPlugin"),
                        "Core file [" + javaFile.getFileName() + "] contains hardcoded plugin reference: GreetingPlugin");
                assertFalse(content.contains("JiraPlugin"),
                        "Core file [" + javaFile.getFileName() + "] contains hardcoded plugin reference: JiraPlugin");
            }
        }
    }

    @Test
    public void testExternalPluginExampleDependsOnlyOnApi() throws Exception {
        Path pomPath = Path.of("../agentx-plugin-example/pom.xml");
        if (!Files.exists(pomPath)) {
            pomPath = Path.of("agentx-plugin-example/pom.xml");
        }
        assertTrue(Files.exists(pomPath), "agentx-plugin-example/pom.xml must exist");

        String pomContent = Files.readString(pomPath);
        assertTrue(pomContent.contains("<artifactId>agentx-api</artifactId>"), "Plugin example must depend on agentx-api");
        assertFalse(pomContent.contains("<artifactId>agentx-core</artifactId>"), "Plugin example must NOT depend on agentx-core");
        assertFalse(pomContent.contains("<artifactId>agentx-mcp</artifactId>"), "Plugin example must NOT depend on agentx-mcp");
    }
}
