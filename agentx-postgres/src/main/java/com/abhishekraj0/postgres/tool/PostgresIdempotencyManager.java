package com.abhishekraj0.postgres.tool;

import com.abhishekraj0.api.tool.IdempotencyDecision;
import com.abhishekraj0.api.tool.IdempotencyManager;
import com.abhishekraj0.api.tool.ToolExecutionRequest;
import com.abhishekraj0.api.tool.ToolExecutionResult;

import javax.sql.DataSource;
import java.sql.*;

public class PostgresIdempotencyManager implements IdempotencyManager {

    private final DataSource dataSource;

    public PostgresIdempotencyManager(DataSource dataSource) {
        this.dataSource = dataSource;
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
        String sql = "SELECT output, success, error_message FROM agent_tool_execution WHERE idempotency_key = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, request.idempotencyKey());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String output = rs.getString("output");
                    boolean success = rs.getBoolean("success");
                    String errorMessage = rs.getString("error_message");
                    return IdempotencyDecision.useCached(output, success, errorMessage);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check idempotency key " + request.idempotencyKey(), e);
        }
        return IdempotencyDecision.executeNew();
    }

    @Override
    public void record(ToolExecutionResult result) {
        if (result == null || result.idempotencyKey() == null) {
            return;
        }
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
            ps.setString(1, result.idempotencyKey());
            ps.setString(2, result.executionId());
            ps.setString(3, result.toolCallId());
            ps.setString(4, result.toolId());
            ps.setBoolean(5, result.success());
            ps.setString(6, result.output());
            ps.setString(7, result.errorMessage());
            ps.setTimestamp(8, result.completedAt() != null ? Timestamp.from(result.completedAt()) : null);
            ps.setString(9, result.status());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record idempotency result for key " + result.idempotencyKey(), e);
        }
    }
}
