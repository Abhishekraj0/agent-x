package com.abhishekraj0.postgres.store;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.loop.GoalStatus;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.security.ApprovalResult;
import com.abhishekraj0.api.security.PermissionManager;
import com.abhishekraj0.api.tool.*;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.agent.DefaultCheckpointManager;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import com.abhishekraj0.core.tool.FunctionTool;
import com.abhishekraj0.postgres.tool.PostgresIdempotencyManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class PostgresDurableExecutionTest {

    private static PostgreSQLContainer<?> postgres;
    private static PGSimpleDataSource dataSource;
    private static boolean usingTestcontainers = true;

    @BeforeAll
    public static void setUpAll() {
        try {
            System.setProperty("testcontainers.ryuk.disabled", "true");
            System.setProperty("docker.host", "unix:///var/run/docker.sock");
            postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("agentx_test")
                    .withUsername("test")
                    .withPassword("test");
            postgres.start();
            dataSource = new PGSimpleDataSource();
            dataSource.setURL(postgres.getJdbcUrl());
            dataSource.setUser(postgres.getUsername());
            dataSource.setPassword(postgres.getPassword());
            System.out.println("Testcontainers started successfully.");
        } catch (Throwable t) {
            System.err.println("Testcontainers initialization failed. Falling back to local PostgreSQL database. Details: " + t.getMessage());
            usingTestcontainers = false;
            dataSource = new PGSimpleDataSource();
            dataSource.setURL("jdbc:postgresql://localhost:5432/knotticles_crm");
            dataSource.setUser("postgres");
            dataSource.setPassword("password");

            try (Connection conn = dataSource.getConnection()) {
                System.out.println("Successfully connected to local fallback PostgreSQL database.");
            } catch (Exception ex) {
                throw new RuntimeException("Both Testcontainers and local fallback Postgres failed.", t);
            }
        }
    }

    @AfterAll
    public static void tearDownAll() {
        if (usingTestcontainers && postgres != null) {
            try {
                postgres.stop();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @BeforeEach
    public void cleanTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS agent_execution_snapshot CASCADE");
            stmt.execute("DROP TABLE IF EXISTS agent_tool_execution CASCADE");
            System.out.println("Cleaned test tables successfully.");
        } catch (SQLException e) {
            System.err.println("Failed to clean test tables: " + e.getMessage());
        }
    }

    @Test
    public void testPostgresExecutionStoreBasicOps() {
        PostgresAgentExecutionStore store = new PostgresAgentExecutionStore(dataSource);
        String execId = UUID.randomUUID().toString();

        AgentState state = new AgentState(execId, List.of(), null, java.util.Map.of(), 0, 0, "INITIALIZING");
        AgentExecutionSnapshot snapshot = new AgentExecutionSnapshot(
                execId,
                "agent-1",
                "Buy milk",
                state,
                "RUNNING",
                null,
                1,
                0,
                List.of(),
                List.of(),
                null,
                "NONE",
                java.util.Map.of(),
                java.time.Instant.now(),
                java.util.Map.of()
        );

        store.save(snapshot);

        Optional<AgentExecutionSnapshot> found = store.find(execId);
        assertTrue(found.isPresent());
        assertEquals("Buy milk", found.get().goal());
        assertEquals("RUNNING", found.get().loopState());

        store.delete(execId);
        assertFalse(store.find(execId).isPresent());
    }

    @Test
    public void testPostgresIdempotencyOps() {
        PostgresIdempotencyManager idempotency = new PostgresIdempotencyManager(dataSource);
        String key = "key-" + UUID.randomUUID();

        ToolExecutionRequest req = new ToolExecutionRequest(
                "exec-1",
                "call-1",
                "tool-1",
                1,
                key,
                java.time.Instant.now()
        );

        IdempotencyDecision dec1 = idempotency.check(req);
        assertFalse(dec1.isDuplicate());

        ToolExecutionResult res = new ToolExecutionResult(
                "exec-1",
                "call-1",
                "tool-1",
                key,
                true,
                "Result output",
                null,
                java.time.Instant.now(),
                "COMPLETED"
        );
        idempotency.record(res);

        IdempotencyDecision dec2 = idempotency.check(req);
        assertTrue(dec2.isDuplicate());
        assertEquals("Result output", dec2.cachedOutput());
    }

    @Test
    public void testPostgresProcessRestartGoldenTest() {
        String executionId = UUID.randomUUID().toString();
        
        // Setup shared databases / managers
        PostgresAgentExecutionStore store1 = new PostgresAgentExecutionStore(dataSource);
        DefaultCheckpointManager checkpointManager1 = new DefaultCheckpointManager();
        PostgresIdempotencyManager idempotencyManager1 = new PostgresIdempotencyManager(dataSource);

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        AtomicInteger toolCallCount = new AtomicInteger(0);

        ToolId toolId = new ToolId("secure_payment");
        ToolMetadata metadataObj = new ToolMetadata(
                RiskLevel.HIGH,
                true, // requiresApproval
                false, // readOnly
                false, // idempotent
                Duration.ofSeconds(30)
        );

        AgentTool paymentTool = new FunctionTool(
                toolId,
                "Requires approval before sending payment",
                ToolSchema.empty(),
                context -> {
                    toolCallCount.incrementAndGet();
                    return ToolResult.success("Payment of $100 succeeded");
                },
                metadataObj
        );
        toolRegistry.register(paymentTool);

        MockChatModel model = new MockChatModel();
        model.setHandler(request -> {
            List<ChatMessage> history = request.messages();
            boolean hasToolResponse = history.stream()
                    .anyMatch(msg -> msg.role() == ChatMessageRole.TOOL);

            if (!hasToolResponse) {
                ToolCall call = new ToolCall("pay-1", "secure_payment", "{}");
                return new ChatResponse(
                        ChatMessage.assistant(null, List.of(call)),
                        new TokenUsage(5, 5, 10),
                        "TOOL_CALL"
                );
            } else {
                return new ChatResponse(
                        ChatMessage.assistant("The payment is complete and verified."),
                        new TokenUsage(10, 5, 15),
                        "STOP"
                );
            }
        });

        // Set up PermissionManager to require approval
        PermissionManager permissionManager = (action, context) ->
                new com.abhishekraj0.api.security.PermissionDecision(
                        com.abhishekraj0.api.security.PermissionStatus.REQUIRE_APPROVAL,
                        "Payment requires human verification"
                );

        // 1. Initial build and run of the Agent using Runtime Instance A
        Agent agentInstance1 = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .permissionManager(permissionManager)
                .executionStore(store1)
                .checkpointManager(checkpointManager1)
                .idempotencyManager(idempotencyManager1)
                .goalEvaluator(state -> {
                    if (state.history().stream().anyMatch(msg -> msg.content() != null && msg.content().contains("complete and verified"))) {
                        return GoalStatus.COMPLETE;
                    }
                    return GoalStatus.IN_PROGRESS;
                })
                .build();

        AgentRequest request = new AgentRequest("Pay $100 to merchant", executionId, AgentOptions.defaultOptions());
        AgentResponse response1 = agentInstance1.run(request);

        // Verify that the run paused synchronously and returned waiting for approval
        assertNotNull(response1);
        assertEquals("WAITING_APPROVAL", response1.state().status());
        assertEquals(0, toolCallCount.get(), "Tool should not be called yet because it is waiting for approval");

        // Verify that snapshot has been saved to PostgreSQL
        Optional<AgentExecutionSnapshot> savedSnapshot = store1.find(executionId);
        assertTrue(savedSnapshot.isPresent());
        assertEquals("WAITING_FOR_APPROVAL", savedSnapshot.get().loopState());
        assertNotNull(savedSnapshot.get().pendingDecision());

        // 2. MOCK A CRASH: Completely discard agentInstance1, store1, checkpointManager1, and idempotencyManager1.
        // We recreate completely new instances pointing to the same postgres database.
        PostgresAgentExecutionStore store2 = new PostgresAgentExecutionStore(dataSource);
        DefaultCheckpointManager checkpointManager2 = new DefaultCheckpointManager();
        PostgresIdempotencyManager idempotencyManager2 = new PostgresIdempotencyManager(dataSource);

        Agent agentInstance2 = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .permissionManager(permissionManager)
                .executionStore(store2)
                .checkpointManager(checkpointManager2)
                .idempotencyManager(idempotencyManager2)
                .goalEvaluator(state -> {
                    if (state.history().stream().anyMatch(msg -> msg.content() != null && msg.content().contains("complete and verified"))) {
                        return GoalStatus.COMPLETE;
                    }
                    return GoalStatus.IN_PROGRESS;
                })
                .build();

        // 3. Resume the execution with approval granted
        ResumeInput resumeInput = ResumeInput.ofApproval(new ApprovalResult(true, "Admin", "Payment approved by Admin"));

        // Call resume on the new instance
        assertTrue(agentInstance2 instanceof ResumableAgentRuntime);
        AgentResponse response2 = ((ResumableAgentRuntime) agentInstance2).resume(executionId, resumeInput);

        // Verify that the agent completed successfully after resuming
        assertNotNull(response2);
        assertEquals("COMPLETED", response2.state().status());
        assertEquals(1, toolCallCount.get(), "Tool should have been called exactly once upon resuming");
        assertTrue(response2.output().contains("complete and verified"));

        // Verify that the execution store is updated in PostgreSQL with the completed state
        Optional<AgentExecutionSnapshot> finalSnapshot = store2.find(executionId);
        assertTrue(finalSnapshot.isPresent());
        assertEquals("COMPLETED", finalSnapshot.get().loopState());
    }

    @Test
    public void testDatabaseConcurrencyConflict() {
        PostgresAgentExecutionStore store = new PostgresAgentExecutionStore(dataSource);
        String execId = UUID.randomUUID().toString();

        AgentState state = new AgentState(execId, List.of(), null, java.util.Map.of(), 0, 0, "INITIALIZING");
        AgentExecutionSnapshot snapshot = new AgentExecutionSnapshot(
                execId,
                "agent-1",
                "Buy milk",
                state,
                "RUNNING",
                null,
                1,
                0,
                List.of(),
                List.of(),
                null,
                "NONE",
                java.util.Map.of(),
                java.time.Instant.now(),
                java.util.Map.of(),
                0
        );

        // First save creates the record with version 1
        store.save(snapshot);

        // Load two instances of the snapshot representing two runtimes (both with version 1)
        Optional<AgentExecutionSnapshot> snapA = store.find(execId);
        Optional<AgentExecutionSnapshot> snapB = store.find(execId);
        assertTrue(snapA.isPresent());
        assertTrue(snapB.isPresent());
        assertEquals(1, snapA.get().version());
        assertEquals(1, snapB.get().version());

        // Runtime A updates: version changes from 1 to 2
        AgentExecutionSnapshot updateA = new AgentExecutionSnapshot(
                execId, "agent-1", "Buy milk", state, "RUNNING", null, 1, 0,
                List.of(), List.of(), null, "NONE", java.util.Map.of(),
                java.time.Instant.now(), java.util.Map.of(), snapA.get().version()
        );
        store.save(updateA);

        // Verify version is now 2 in DB
        Optional<AgentExecutionSnapshot> snapUpdated = store.find(execId);
        assertEquals(2, snapUpdated.get().version());

        // Runtime B tries to update using version 1 snapshot: must fail with ConcurrentModificationException
        AgentExecutionSnapshot updateB = new AgentExecutionSnapshot(
                execId, "agent-1", "Buy milk", state, "RUNNING", null, 1, 0,
                List.of(), List.of(), null, "NONE", java.util.Map.of(),
                java.time.Instant.now(), java.util.Map.of(), snapB.get().version()
        );
        assertThrows(java.util.ConcurrentModificationException.class, () -> store.save(updateB));
    }

    @Test
    public void testConcurrentResumeConflict() {
        PostgresAgentExecutionStore store = new PostgresAgentExecutionStore(dataSource);
        String execId = UUID.randomUUID().toString();

        AgentState state = new AgentState(execId, List.of(), null, java.util.Map.of("pending_tool_call_id", "pay-1"), 0, 0, "WAITING_APPROVAL");
        AgentExecutionSnapshot snapshot = new AgentExecutionSnapshot(
                execId,
                "agent-1",
                "Buy milk",
                state,
                "WAITING_APPROVAL",
                null,
                1,
                0,
                List.of(),
                List.of(),
                null,
                "NONE",
                java.util.Map.of(),
                java.time.Instant.now(),
                java.util.Map.of(),
                0
        );
        store.save(snapshot);

        MockChatModel model = new MockChatModel();
        model.setHandler(req -> new ChatResponse(ChatMessage.assistant("Done"), new TokenUsage(1, 1, 2), "STOP"));

        Agent runtimeA = AgentX.builder()
                .model(model)
                .executionStore(store)
                .goalEvaluator(s -> GoalStatus.COMPLETE)
                .build();

        Agent runtimeB = AgentX.builder()
                .model(model)
                .executionStore(store)
                .goalEvaluator(s -> GoalStatus.COMPLETE)
                .build();

        // Simulate Runtime A resuming: it loads version 1, saves running status, and completes.
        ResumeInput input = ResumeInput.ofApproval(new ApprovalResult(true, "Admin", "Payment approved"));
        AgentResponse resA = ((ResumableAgentRuntime) runtimeA).resume(execId, input);
        assertEquals("COMPLETED", resA.state().status());

        // Now, if we manually try to save a snapshot with a stale version to simulate a concurrent write
        // during B's resume phase:
        AgentExecutionSnapshot staleSnapshot = new AgentExecutionSnapshot(
                execId, "agent-1", "Buy milk", state, "RUNNING", null, 1, 0,
                List.of(), List.of(), null, "NONE", java.util.Map.of(),
                java.time.Instant.now(), java.util.Map.of(), 1 // Stale version 1
        );

        assertThrows(java.util.ConcurrentModificationException.class, () -> store.save(staleSnapshot));
    }
}
