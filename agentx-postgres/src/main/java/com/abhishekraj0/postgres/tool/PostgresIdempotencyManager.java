package com.abhishekraj0.postgres.tool;

import com.abhishekraj0.api.security.SecretRedactor;
import com.abhishekraj0.api.tool.IdempotencyDecision;
import com.abhishekraj0.api.tool.IdempotencyManager;
import com.abhishekraj0.api.tool.ToolExecutionRequest;
import com.abhishekraj0.api.tool.ToolExecutionResult;

import javax.sql.DataSource;
import java.sql.*;

public class PostgresIdempotencyManager implements IdempotencyManager {

    private final DataSource dataSource;
    private final SecretRedactor secretRedactor;

    public PostgresIdempotencyManager(DataSource dataSource) {
        this(dataSource, text -> text);
    }

    public PostgresIdempotencyManager(DataSource dataSource, SecretRedactor secretRedactor) {
        this.dataSource = dataSource;
        this.secretRedactor = secretRedactor != null ? secretRedactor : text -> text;
        initializeDatabase();
    }

    private void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS agent_tool_execution (" +
                "  idempotency_key VARCHAR(255) PRIMARY KEY," +
                "  execution_id VARCHAR(255)," +
                "  tool_call_id VARCHAR(255)," +
                "  tool_id VARCHAR(255)," +
                "  success BOOLEAN," +
                "  output TEXT," +
                "  error_message TEXT," +
                "  completed_at TIMESTAMP," +
                "  status VARCHAR(255)" +
                ")";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database table agent_tool_execution", e);
        }
    }

    @Override
    public IdempotencyDecision check(ToolExecutionRequest request) {
        if (request == null || request.idempotencyKey() == null) {
            return IdempotencyDecision.executeNew();
        }
        String cleanKey = secretRedactor.redact(request.idempotencyKey());
        String sql = "SELECT output, success, error_message, status FROM agent_tool_execution WHERE idempotency_key = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    if ("PENDING".equals(status)) {
                        return IdempotencyDecision.unknownResult("Tool execution outcome is unknown due to prior interruption or unconfirmed completion");
                    }
                    String output = rs.getString("output");
                    boolean success = rs.getBoolean("success");
                    String errorMessage = rs.getString("error_message");
                    return IdempotencyDecision.useCached(output, success, errorMessage);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check idempotency key " + cleanKey, e);
        }
        return IdempotencyDecision.executeNew();
    }

    @Override
    public void recordPending(ToolExecutionRequest request) {
        if (request == null || request.idempotencyKey() == null) {
            return;
        }
        String cleanKey = secretRedactor.redact(request.idempotencyKey());
        String sql = "INSERT INTO agent_tool_execution (" +
                "  idempotency_key, execution_id, tool_call_id, tool_id, success, output, error_message, completed_at, status" +
                ") VALUES (?, ?, ?, ?, false, null, 'Tool execution in progress', ?, 'PENDING') " +
                "ON CONFLICT (idempotency_key) DO NOTHING";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanKey);
            ps.setString(2, request.executionId());
            ps.setString(3, request.toolCallId());
            ps.setString(4, request.toolId());
            ps.setTimestamp(5, Timestamp.from(request.startedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record pending idempotency key " + cleanKey, e);
        }
    }

    @Override
    public void record(ToolExecutionResult result) {
        if (result == null || result.idempotencyKey() == null) {
            return;
        }
        String cleanKey = secretRedactor.redact(result.idempotencyKey());
        String cleanOutput = secretRedactor.redact(result.output());
        String cleanError = secretRedactor.redact(result.errorMessage());

        String sql = "INSERT INTO agent_tool_execution (" +
                "  idempotency_key, execution_id, tool_call_id, tool_id, success, output, error_message, completed_at, status" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (idempotency_key) DO UPDATE SET " +
                "  execution_id = EXCLUDED.execution_id," +
                "  tool_call_id = EXCLUDED.tool_call_id," +
                "  tool_id = EXCLUDED.tool_id," +
                "  success = EXCLUDED.success," +
                "  output = EXCLUDED.output," +
                "  error_message = EXCLUDED.error_message," +
                "  completed_at = EXCLUDED.completed_at," +
                "  status = EXCLUDED.status";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanKey);
            ps.setString(2, result.executionId());
            ps.setString(3, result.toolCallId());
            ps.setString(4, result.toolId());
            ps.setBoolean(5, result.success());
            ps.setString(6, cleanOutput);
            ps.setString(7, cleanError);
            ps.setTimestamp(8, result.completedAt() != null ? Timestamp.from(result.completedAt()) : null);
            ps.setString(9, result.status());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record idempotency result for key " + cleanKey, e);
        }
    }
}
