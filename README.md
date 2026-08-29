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

## 🚀 Key Features

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

## 🛠 How to Use

### 1. Build and Install
Clone the repository and compile using Maven:
```bash
git clone https://github.com/Abhishekraj0/agent-x.git
cd agent-x
mvn clean install
```

### 2. Basic Agent Usage (Core Java)
Create an agent utilizing the fluent builder API:
```java
import com.abhishekraj0.api.agent.Agent;
import com.abhishekraj0.api.agent.AgentRequest;
import com.abhishekraj0.api.agent.AgentResponse;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.model.openai.OpenAIChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;

// Initialize model and tool registry
var chatModel = new OpenAIChatModel("gpt-4o-mini", "your-api-key");
var toolRegistry = new DefaultToolRegistry();

// Build the Agent
Agent agent = AgentX.builder()
        .model(chatModel)
        .tools(toolRegistry)
        .build();

// Execute a request
AgentResponse response = agent.run(new AgentRequest("How can I build workflows in Java?"));
System.out.println("Agent Output: " + response.output());
```

### 3. Orchestrate a Deterministic Workflow
Run sequential and parallel execution paths:
```java
import com.abhishekraj0.api.workflow.WorkflowContext;
import com.abhishekraj0.core.workflow.*;
import java.util.concurrent.ConcurrentHashMap;

// Define steps
WorkflowStep step1 = (context) -> {
    context.variables().put("key1", "value1");
    return context;
};

WorkflowStep step2 = (context) -> {
    context.variables().put("key2", "value2");
    return context;
};

// Create a Parallel step group
var parallelGroup = new ParallelStep("parallel-tasks", step1, step2);

// Execute via DefaultWorkflow
var workflowContext = new WorkflowContext("tx-123", new ConcurrentHashMap<>());
var result = new DefaultWorkflow().execute(parallelGroup, workflowContext);
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

### 3. Submitting Pull Requests
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
