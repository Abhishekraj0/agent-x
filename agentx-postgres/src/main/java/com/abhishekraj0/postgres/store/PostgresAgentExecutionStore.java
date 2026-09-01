package com.abhishekraj0.postgres.store;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.planner.Plan;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PostgresAgentExecutionStore implements AgentExecutionStore {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public PostgresAgentExecutionStore(DataSource dataSource) {
        this.dataSource = dataSource;
        com.fasterxml.jackson.databind.module.SimpleModule module = new com.fasterxml.jackson.databind.module.SimpleModule();
        module.addDeserializer(AgentDecision.class, new AgentDecisionDeserializer());

        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(module)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        initializeDatabase();
    }

    private void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS agent_execution_snapshot (" +
                "  execution_id VARCHAR(255) PRIMARY KEY," +
                "  agent_id VARCHAR(255)," +
                "  goal TEXT," +
                "  state_json TEXT," +
                "  loop_state VARCHAR(255)," +
                "  plan_json TEXT," +
                "  iteration INT," +
                "  tool_call_count INT," +
                "  observations_json TEXT," +
                "  memory_references_json TEXT," +
                "  pending_decision_json TEXT," +
                "  approval_state VARCHAR(255)," +
                "  budgets_json TEXT," +
                "  timestamp TIMESTAMP," +
                "  metadata_json TEXT," +
                "  version INT DEFAULT 1" +
                ")";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database table agent_execution_snapshot", e);
        }
    }

    @Override
    public void save(AgentExecutionSnapshot snapshot) {
        try (Connection conn = dataSource.getConnection()) {
            boolean exists = false;
            int dbVersion = 0;
            String checkSql = "SELECT version FROM agent_execution_snapshot WHERE execution_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, snapshot.executionId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        exists = true;
                        dbVersion = rs.getInt("version");
                    }
                }
            }

            if (!exists) {
                String insertSql = "INSERT INTO agent_execution_snapshot (" +
                        "  execution_id, agent_id, goal, state_json, loop_state, plan_json, iteration, " +
                        "  tool_call_count, observations_json, memory_references_json, pending_decision_json, " +
                        "  approval_state, budgets_json, timestamp, metadata_json, version" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, snapshot.executionId());
                    ps.setString(2, snapshot.agentId());
                    ps.setString(3, snapshot.goal());
                    ps.setString(4, toJson(snapshot.state()));
                    ps.setString(5, snapshot.loopState());
                    ps.setString(6, toJson(snapshot.plan()));
                    ps.setInt(7, snapshot.iteration());
                    ps.setInt(8, snapshot.toolCallCount());
                    ps.setString(9, toJson(snapshot.observations()));
                    ps.setString(10, toJson(snapshot.memoryReferences()));
                    ps.setString(11, toJson(snapshot.pendingDecision()));
                    ps.setString(12, snapshot.approvalState());
                    ps.setString(13, toJson(snapshot.budgets()));
                    ps.setTimestamp(14, snapshot.timestamp() != null ? Timestamp.from(snapshot.timestamp()) : null);
                    ps.setString(15, toJson(snapshot.metadata()));
                    ps.setInt(16, 1);
                    ps.executeUpdate();
                }
            } else {
                if (snapshot.version() > 0 && snapshot.version() != dbVersion) {
                    throw new java.util.ConcurrentModificationException("Optimistic locking failure: expected version " + snapshot.version() + " but database has version " + dbVersion);
                }

                String updateSql = "UPDATE agent_execution_snapshot SET " +
                        "  agent_id = ?," +
                        "  goal = ?," +
                        "  state_json = ?," +
                        "  loop_state = ?," +
                        "  plan_json = ?," +
                        "  iteration = ?," +
                        "  tool_call_count = ?," +
                        "  observations_json = ?," +
                        "  memory_references_json = ?," +
                        "  pending_decision_json = ?," +
                        "  approval_state = ?," +
                        "  budgets_json = ?," +
                        "  timestamp = ?," +
                        "  metadata_json = ?," +
                        "  version = version + 1 " +
                        "WHERE execution_id = ? AND version = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, snapshot.agentId());
                    ps.setString(2, snapshot.goal());
                    ps.setString(3, toJson(snapshot.state()));
                    ps.setString(4, snapshot.loopState());
                    ps.setString(5, toJson(snapshot.plan()));
                    ps.setInt(6, snapshot.iteration());
                    ps.setInt(7, snapshot.toolCallCount());
                    ps.setString(8, toJson(snapshot.observations()));
                    ps.setString(9, toJson(snapshot.memoryReferences()));
                    ps.setString(10, toJson(snapshot.pendingDecision()));
                    ps.setString(11, snapshot.approvalState());
                    ps.setString(12, toJson(snapshot.budgets()));
                    ps.setTimestamp(13, snapshot.timestamp() != null ? Timestamp.from(snapshot.timestamp()) : null);
                    ps.setString(14, toJson(snapshot.metadata()));
                    ps.setString(15, snapshot.executionId());
                    ps.setInt(16, dbVersion);

                    int updated = ps.executeUpdate();
                    if (updated == 0) {
                        throw new java.util.ConcurrentModificationException("Optimistic locking failure: version conflict on update for execution " + snapshot.executionId());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save snapshot " + snapshot.executionId(), e);
        }
    }

    @Override
    public Optional<AgentExecutionSnapshot> find(String executionId) {
        String sql = "SELECT * FROM agent_execution_snapshot WHERE execution_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, executionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AgentState state = fromJson(rs.getString("state_json"), AgentState.class);
                    Plan plan = fromJson(rs.getString("plan_json"), Plan.class);
                    List<String> observations = fromJson(rs.getString("observations_json"), new TypeReference<List<String>>() {});
                    List<String> memoryReferences = fromJson(rs.getString("memory_references_json"), new TypeReference<List<String>>() {});
                    AgentDecision pendingDecision = fromJson(rs.getString("pending_decision_json"), AgentDecision.class);
                    Map<String, Object> budgets = fromJson(rs.getString("budgets_json"), new TypeReference<Map<String, Object>>() {});
                    Map<String, Object> metadata = fromJson(rs.getString("metadata_json"), new TypeReference<Map<String, Object>>() {});
                    Timestamp ts = rs.getTimestamp("timestamp");

                    AgentExecutionSnapshot snapshot = new AgentExecutionSnapshot(
                            rs.getString("execution_id"),
                            rs.getString("agent_id"),
                            rs.getString("goal"),
                            state,
                            rs.getString("loop_state"),
                            plan,
                            rs.getInt("iteration"),
                            rs.getInt("tool_call_count"),
                            observations,
                            memoryReferences,
                            pendingDecision,
                            rs.getString("approval_state"),
                            budgets,
                            ts != null ? ts.toInstant() : null,
                            metadata,
                            rs.getInt("version")
                    );
                    return Optional.of(snapshot);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find snapshot for " + executionId, e);
        }
        return Optional.empty();
    }

    @Override
    public void delete(String executionId) {
        String sql = "DELETE FROM agent_execution_snapshot WHERE execution_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, executionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete snapshot " + executionId, e);
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }

    private static class AgentDecisionDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<AgentDecision> {
        @Override
        public AgentDecision deserialize(com.fasterxml.jackson.core.JsonParser p, com.fasterxml.jackson.databind.DeserializationContext ctxt)
                throws java.io.IOException {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(p);

            if (node.has("approvalRequest")) {
                return mapper.treeToValue(node, WaitForApprovalDecision.class);
            } else if (node.has("toolCalls")) {
                return mapper.treeToValue(node, ToolCallDecision.class);
            } else if (node.has("response")) {
                return mapper.treeToValue(node, FinalResponseDecision.class);
            } else if (node.has("question")) {
                return mapper.treeToValue(node, AskUserDecision.class);
            } else if (node.has("targetAgentId")) {
                return mapper.treeToValue(node, DelegateDecision.class);
            } else if (node.has("replanDetails")) {
                return mapper.treeToValue(node, ReplanDecision.class);
            } else if (node.has("delay")) {
                return mapper.treeToValue(node, RetryDecision.class);
            }

            throw new java.io.IOException("Cannot deserialize AgentDecision: unknown fields in node " + node);
        }
    }
}
