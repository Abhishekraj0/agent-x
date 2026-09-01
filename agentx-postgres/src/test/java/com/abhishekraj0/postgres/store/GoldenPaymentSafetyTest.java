package com.abhishekraj0.postgres.store;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.failure.AgentFailure;
import com.abhishekraj0.api.loop.GoalStatus;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.tool.*;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import com.abhishekraj0.core.tool.FunctionTool;
import com.abhishekraj0.postgres.tool.PostgresIdempotencyManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class GoldenPaymentSafetyTest {

    private static PostgreSQLContainer<?> postgres;
    private static PGSimpleDataSource dataSource;
    private static boolean usingTestcontainers = true;

    private PostgresAgentExecutionStore executionStore;
    private PostgresIdempotencyManager idempotencyManager;
    private DefaultToolRegistry toolRegistry;
    private AtomicInteger externalPaymentGatewayCalls;

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
        } catch (Throwable t) {
            usingTestcontainers = false;
            dataSource = new PGSimpleDataSource();
            dataSource.setURL("jdbc:postgresql://localhost:5432/knotticles_crm");
            dataSource.setUser("postgres");
            dataSource.setPassword("password");
        }
    }

    @AfterAll
    public static void tearDownAll() {
        if (usingTestcontainers && postgres != null) {
            try {
                postgres.stop();
            } catch (Exception ignored) {}
        }
    }

    @BeforeEach
    public void setUp() {
        externalPaymentGatewayCalls = new AtomicInteger(0);
        cleanDatabase();
        this.executionStore = new PostgresAgentExecutionStore(dataSource);
        this.idempotencyManager = new PostgresIdempotencyManager(dataSource);
        this.toolRegistry = new DefaultToolRegistry();

        // Register non-idempotent payment tool
        toolRegistry.register(new FunctionTool(
                new ToolId("processPayment"),
                "Process external customer payment",
                ToolSchema.empty(),
                c -> {
                    externalPaymentGatewayCalls.incrementAndGet();
                    return ToolResult.success("tx_gw_99998888");
                },
                new ToolMetadata(RiskLevel.HIGH, false, false, false, false, Duration.ofSeconds(5)) // safeAfterUnknownResult = false
        ));
    }

    private void cleanDatabase() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS agent_execution_snapshot");
            stmt.execute("DROP TABLE IF EXISTS agent_tool_execution");
        } catch (Exception ignored) {}
    }

    @Test
    public void testGoldenPaymentCrashRecoveryAndOperatorResolution() {
        String execId = UUID.randomUUID().toString();
        String idempotencyKey = "processPayment_" + "{\"amount\":1000}";

        // Step 1: Simulate initial payment attempt where tool execution request was marked PENDING in PostgreSQL
        // but crash occurred before completion was recorded.
        ToolExecutionRequest req = new ToolExecutionRequest(execId, "call-pay-001", "processPayment", 1, idempotencyKey, java.time.Instant.now());
        idempotencyManager.recordPending(req);
        externalPaymentGatewayCalls.set(1); // 1 external call occurred prior to crash

        MockChatModel model = new MockChatModel();
        model.setHandler(r -> {
            boolean hasObs = r.messages().stream().anyMatch(m -> m.content() != null && m.content().contains("tx_gw_99998888"));
            if (hasObs) {
                return new ChatResponse(ChatMessage.assistant("Payment processed successfully"), new TokenUsage(5, 5, 10), "STOP");
            }
            return new ChatResponse(
                    ChatMessage.assistant(null, List.of(new ToolCall("call-pay-001", "processPayment", "{\"amount\":1000}"))),
                    new TokenUsage(5, 5, 10),
                    "TOOL_USE"
            );
        });

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .idempotencyManager(idempotencyManager)
                .executionStore(executionStore)
                .goalEvaluator(s -> s.history().stream().anyMatch(m -> m.content() != null && m.content().contains("Payment processed successfully")) ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS)
                .build();

        // Step 2: New JVM / Runtime starts and attempts to execute request.
        // FAIL_SAFE policy default MUST prevent second payment execution!
        AgentFailure failure = assertThrows(AgentFailure.class, () ->
                ((AgentRuntime) agent).execute(new AgentRequest("Pay $1000", execId, AgentOptions.defaultOptions()))
        );

        assertEquals("UNKNOWN_TOOL_RESULT", failure.getCode());
        assertEquals(1, externalPaymentGatewayCalls.get(), "AgentX MUST NOT issue a second payment request after unknown outcome!");

        // Step 3: Human operator inspects payment gateway, verifies tx_gw_99998888 succeeded, and provides CONFIRMED_SUCCESS resolution.
        Map<String, Object> vars = new HashMap<>();
        vars.put("unknown_result_resolution_call-pay-001", UnknownResultResolution.confirmedSuccess("tx_gw_99998888", "Payment confirmed on gateway dashboard"));

        AgentOptions resolveOptions = new AgentOptions(10, 20, Duration.ofMinutes(5), 0.7, Map.of());
        AgentResponse response = ((AgentRuntime) agent).execute(new AgentRequest("Pay $1000", execId, resolveOptions, vars));

        // Step 4: Verify execution succeeded without ANY duplicate payment calls!
        assertEquals("COMPLETED", response.state().status());
        assertEquals(1, externalPaymentGatewayCalls.get(), "Payment tool must NEVER be re-invoked when operator confirms success!");
    }
}
