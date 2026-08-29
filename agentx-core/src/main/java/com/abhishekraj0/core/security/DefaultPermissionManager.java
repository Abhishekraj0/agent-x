package com.abhishekraj0.core.security;

import com.abhishekraj0.api.agent.AgentAction;
import com.abhishekraj0.api.context.AgentContext;
import com.abhishekraj0.api.security.PermissionDecision;
import com.abhishekraj0.api.security.PermissionManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Customizable permission manager mapping action names/tool names to specific permission decisions.
 */
public class DefaultPermissionManager implements PermissionManager {

    private final Map<String, PermissionDecision> rules = new HashMap<>();
    private PermissionDecision defaultDecision = PermissionDecision.allow();

    public DefaultPermissionManager() {
    }

    public DefaultPermissionManager(PermissionDecision defaultDecision) {
        this.defaultDecision = defaultDecision;
    }

    public void grant(String actionOrToolName) {
        rules.put(actionOrToolName, PermissionDecision.allow());
    }

    public void deny(String actionOrToolName, String reason) {
        rules.put(actionOrToolName, PermissionDecision.deny(reason));
    }

    public void requireApproval(String actionOrToolName, String reason) {
        rules.put(actionOrToolName, PermissionDecision.requireApproval(reason));
    }

    @Override
    public PermissionDecision check(AgentAction action, AgentContext context) {
        String toolName = (String) action.details().get("toolName");
        if (toolName != null && rules.containsKey(toolName)) {
            return rules.get(toolName);
        }
        if (rules.containsKey(action.type())) {
            return rules.get(action.type());
        }
        return defaultDecision;
    }
}
