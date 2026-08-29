# AgentX — Autonomous Open-Source Agentic AI Framework for Java

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://www.oracle.com/java/technologies/downloads/)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()

**AgentX** is a production-oriented, open-source, provider-agnostic **Agentic AI runtime and plugin SDK for Java 21+**. It allows developers to seamlessly embed autonomous AI agents into their Java/Spring Boot applications and extend them with Java tools, Model Context Protocol (MCP) servers, security policies, guardrails, short/long-term memory, and complex deterministic workflows.

---

## 🎯 Goal

The goal of **AgentX** is to provide Java developers with a lightweight, robust, interface-first agentic framework that does not lock them into any single AI provider, memory vendor, or framework. It prioritizes:
1. **Interface-first architecture**: Every major capability (Models, Memory, Tools, Planners) is fully pluggable and replaceable.
2. **Modern Concurrency**: Built-in support for Java 21 Virtual Threads to run parallel workflow tasks efficiently.
3. **Execution Safety**: Dual-phase security validation (pre-execution rules + human-in-the-loop approvals).
4. **Zero-Lockin**: Run standalone core Java or integrate optionally with Spring Boot.

---

## 📦 How to Use the Package in Your Project

### 1. Configure Repository in `pom.xml`
Because the package is hosted on GitHub Packages, configure the repository in your consuming Maven project:
```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/Abhishekraj0/agent-x</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

### 2. Configure Authentication in `~/.m2/settings.xml`
Generate a Personal Access Token (PAT) on GitHub with `read:packages` scope and configure your local Maven settings:
```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_PERSONAL_ACCESS_TOKEN</password>
        </server>
    </servers>
</settings>
```

### 3. Add Maven Dependency
Add the core dependency to your project:
```xml
<dependency>
    <groupId>com.abhishekraj0</groupId>
    <artifactId>agentx-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## 🔑 Environment Variables & Configuration

Below are the key environment variables you can pass to configure AgentX models and integrations:

| Environment Variable | Description | Default / Example Value |
|----------------------|-------------|-------------------------|
| `OPENAI_API_KEY`     | Secret key for authenticating OpenAI calls. | `sk-proj-...` |
| `OLLAMA_API_URL`     | Host URL of your locally running Ollama instance. | `http://localhost:11434` |
| `ANTHROPIC_API_KEY`  | API key for Anthropic integration (used in custom clients). | `sk-ant-...` |
| `GEMINI_API_KEY`     | API key for Google Gemini (used in custom clients). | `AIzaSy...` |

---

## 🤖 How to Integrate Different LLM Providers

AgentX utilizes an interface-first model layer. If you use one of the built-in providers (OpenAI, Ollama) or want to integrate custom ones (Anthropic, Gemini, etc.), follow the code examples below:

### 1. OpenAI
Use the built-in `OpenAIChatModel` adapter:
```java
import com.abhishekraj0.core.model.openai.OpenAIChatModel;

// Reads OPENAI_API_KEY from environment variables by default
var model = new OpenAIChatModel("gpt-4o-mini", System.getenv("OPENAI_API_KEY"));
```

### 2. Ollama (Local Models)
Use the built-in `OllamaChatModel` adapter:
```java
import com.abhishekraj0.core.model.ollama.OllamaChatModel;

// Reads OLLAMA_API_URL or defaults to localhost
var model = new OllamaChatModel("llama3", "http://localhost:11434");
```

### 3. Custom LLM / Any Model Provider
To integrate any model provider (e.g. Anthropic, Gemini, or a custom internal API gateway), simply implement the `ChatModel` interface:
```java
import com.abhishekraj0.api.model.ChatModel;
import com.abhishekraj0.api.model.ChatRequest;
import com.abhishekraj0.api.model.ChatResponse;
import com.abhishekraj0.api.model.ChatMessage;
import com.abhishekraj0.api.model.ChatMessageRole;
import com.abhishekraj0.api.model.ModelMetadata;
import com.abhishekraj0.api.model.TokenUsage;

import java.util.List;

public class CustomChatModel implements ChatModel {
    private final String modelName;
    private final String apiKey;

    public CustomChatModel(String modelName, String apiKey) {
        this.modelName = modelName;
        this.apiKey = apiKey;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        // Implement your custom API call here (e.g. to Anthropic or Google Gemini REST API)
        // String responseText = callMyLlmProvider(request.messages(), this.apiKey);

        var responseMessage = new ChatMessage(
            ChatMessageRole.ASSISTANT, 
            "Hello, this is a response from my custom LLM!"
        );
        
        return new ChatResponse(
            responseMessage,
            new TokenUsage(100, 50, 150),
            List.of()
        );
    }

    @Override
    public ModelMetadata metadata() {
        return new ModelMetadata(modelName, 0.0);
    }
}
```

### 4. How to Use Your Custom Model with the Agent
Once implemented, pass the instance to `AgentX.builder()`:
```java
import com.abhishekraj0.api.agent.Agent;
import com.abhishekraj0.core.AgentX;

Agent agent = AgentX.builder()
        .model(new CustomChatModel("my-custom-llm", "my-key"))
        .build();

var response = agent.run("Perform custom analysis");
System.out.println(response.output());
```

---

## 🛠 Features

*   **Provider-Agnostic LLM Client**: Built-in support for **OpenAI** (cloud) and **Ollama** (local execution) using native Java HTTP client.
*   **Deterministic Workflows**: Orchestrate multi-step execution flows using:
    *   `SequentialStep`: Linear flow sharing context state.
    *   `ParallelStep`: Concurrent task execution utilizing Java 21 Virtual Threads.
    *   `ConditionStep`: Predicate-based conditional branching.
*   **Observability & Telemetry**: Event-driven execution tracing powered by `SimpleEventBus` supporting subscriber-routed events (`ExecutionStartedEvent`, `ToolCalledEvent`, `ExecutionCompletedEvent`).
*   **Security & Guardrails**: Combine static guardrail policies (`CompositeGuardrail`) with interactive human-in-the-loop approvals (`SimpleApprovalProvider`).
*   **Adaptive Memory**: In-memory vector memory store (`InMemoryVectorMemoryStore`) with Cosine similarity searching and token budget management (`DefaultTokenBudgetManager`).
*   **Model Context Protocol (MCP)**: First-class adapter support to import external MCP tools directly as native agent tools.
*   **SPI Plugin Architecture**: Discover and load external modules automatically using Java's `ServiceLoader` SPI.

---

## 📦 Project Structure

```text
agentx/
├── agentx-api/        # Core interfaces, records, and extension contracts
├── agentx-core/       # Implementation details (Model clients, event bus, memory, workflow engine)
├── agentx-mcp/        # Adapter and wrapper classes for MCP servers and tools
├── agentx-spring/     # Spring Boot autoconfiguration and properties mapping
└── agentx-examples/   # Spring Boot CommandLineRunner demo application
```

---

## 🤝 How to Contribute

We welcome contributions to AgentX! Please read the contribution instructions below to get started:

### 1. Fork and Clone
*   Fork the repository on GitHub.
*   Clone your fork locally:
    ```bash
    git clone https://github.com/YOUR_USERNAME/agent-x.git
    ```
*   Create a clean feature branch:
    ```bash
    git checkout -b feature/your-awesome-feature
    ```

### 2. Follow Development Standards
*   Ensure your code targets **Java 21+**.
*   Write unit or integration tests for any new behavior or fixes.
*   Run the verification build before committing:
    ```bash
    mvn clean test
    ```
*   Maintain package prefixing using **`com.abhishekraj0`**.

### 3. Branching & Contribution Policy
*   **No Direct Commits**: Direct commits to `main`, `develop`, and `release` branches are strictly prohibited. All changes must be made in feature branches and merged via Pull Requests.
*   **Branch Protection**: Deletion of the `main`, `develop`, and `release` branches is prohibited and protected by repository policies.

### 4. Submitting Pull Requests
*   Push your branch to your origin fork.
*   Open a Pull Request (PR) from your feature branch targeting the `develop` branch of `Abhishekraj0/agent-x`.
*   Provide a clear PR description detailing what was changed and why.

---

## 🐛 How to Add Issues

If you encounter any bugs, have feature requests, or find documentation gaps:
1. Navigate to the **Issues** tab of the repository: [https://github.com/Abhishekraj0/agent-x/issues](https://github.com/Abhishekraj0/agent-x/issues)
2. Click **New Issue**.
3. Use a clear, descriptive title.
4. Provide a detailed description including:
   * **Steps to Reproduce** (if it's a bug).
   * **Expected Behavior vs. Actual Behavior**.
   * **Environment Details** (Java version, Spring Boot version, OS).
   * Relevant stack traces or log outputs.

---

## 📄 License

AgentX is open-source software licensed under the [Apache License, Version 2.0](LICENSE).
