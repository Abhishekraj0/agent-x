package com.abhishekraj0.postgres.store;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.failure.AgentFailure;
import com.abhishekraj0.api.loop.*;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.security.ApprovalResult;
import com.abhishekraj0.api.security.PermissionDecision;
import com.abhishekraj0.api.security.PermissionManager;
import com.abhishekraj0.api.security.PermissionStatus;
import com.abhishekraj0.api.tool.*;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import com.abhishekraj0.core.tool.FunctionTool;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresConcurrentResumeValidationTest {

    private static PostgreSQLContainer<?> postgres;
    private static PGSimpleDataSource dataSource;
    private static PostgresAgentExecutionStore store;

    @BeforeAll
    public static void setUp() {
        dataSource = new PGSimpleDataSource();
        try {
            System.setProperty("testcontainers.ryuk.disabled", "true");
            postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("agentx_test")
                    .withUsername("test")
                    .withPassword("test");
            postgres.start();
            dataSource.setURL(postgres.getJdbcUrl());
            dataSource.setUser(postgres.getUsername());
            dataSource.setPassword(postgres.getPassword());
        } catch (Throwable t) {
            System.err.println("Testcontainers initialization failed. Falling back to local PostgreSQL database.");
            dataSource.setURL("jdbc:postgresql://localhost:5432/knotticles_crm");
            dataSource.setUser("postgres");
            dataSource.setPassword("password");
        }
        store = new PostgresAgentExecutionStore(dataSource);
    }

    @AfterAll
    public static void tearDown() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @BeforeEach
    public void cleanDatabase() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE agent_execution_snapshot");
        }
    }

    @Test
    public void testPostgres100ConcurrentResumes_ExactlyOneOwnerAndOneToolExecution() throws Exception {
        String execId = UUID.randomUUID().toString();
        AtomicInteger toolInvocations = new AtomicInteger(0);

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("pg-stress-tool"),
                "pg stress tool",
                ToolSchema.empty(),
                c -> {
                    toolInvocations.incrementAndGet();
                    return ToolResult.success("pg ok");
                },
                new ToolMetadata(RiskLevel.HIGH, true, false, false, Duration.ofSeconds(5))
        ));

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> {
            boolean done = req.messages().stream().anyMatch(m -> m.content() != null && m.content().contains("pg ok"));
            if (done) {
                return new ChatResponse(ChatMessage.assistant("Done"), new TokenUsage(5, 5, 10), "STOP");
            }
            return new ChatResponse(ChatMessage.assistant(null, List.of(new ToolCall("call-1", "pg-stress-tool", "{}"))), new TokenUsage(5, 5, 10), "TOOL_USE");
        });

        PermissionManager permissionManager = (action, context) ->
                new PermissionDecision(PermissionStatus.REQUIRE_APPROVAL, "Requires approval");

        int concurrency = 10;
        List<Agent> agents = new ArrayList<>();
        for (int i = 0; i < concurrency; i++) {
            Agent agent = AgentX.builder()
                    .model(model)
                    .tools(toolRegistry)
                    .permissionManager(permissionManager)
                    .executionStore(store)
                    .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("Done")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                    .build();
            agents.add(agent);
        }

        // Setup execution in WAITING_APPROVAL
        AgentResponse initialResponse = ((AgentRuntime) agents.get(0)).execute(new AgentRequest("PG Stress task", execId, AgentOptions.defaultOptions()));
        assertEquals("WAITING_APPROVAL", initialResponse.state().status());

        CountDownLatch startBarrier = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        List<Future<Object>> futures = new ArrayList<>();

        ResumeInput input = ResumeInput.ofApproval(new ApprovalResult(true, "user", "ok"));

        for (Agent a : agents) {
            futures.add(executor.submit(() -> {
                startBarrier.await();
                return ((ResumableAgentRuntime) a).resume(execId, input);
            }));
        }

        startBarrier.countDown();

        int ownerCount = 0;
        int rejectedCount = 0;

        for (Future<Object> f : futures) {
            try {
                Object res = f.get(10, TimeUnit.SECONDS);
                if (res instanceof AgentResponse) {
                    ownerCount++;
                }
            } catch (ExecutionException ee) {
                if (ee.getCause() instanceof AgentFailure af && ("EXECUTION_ALREADY_RUNNING".equals(af.getCode()) || "INVALID_STATUS".equals(af.getCode()))) {
                    rejectedCount++;
                } else if (ee.getCause() instanceof java.util.ConcurrentModificationException) {
                    rejectedCount++;
                }
            }
        }

        executor.shutdownNow();

        assertEquals(1, ownerCount, "Exactly 1 runtime owns and completes PostgreSQL execution");
        assertEquals(concurrency - 1, rejectedCount, "All other runtimes rejected by PostgreSQL optimistic locking");
        assertEquals(1, toolInvocations.get(), "Tool must be invoked exactly ONCE in PostgreSQL test");
    }
}
