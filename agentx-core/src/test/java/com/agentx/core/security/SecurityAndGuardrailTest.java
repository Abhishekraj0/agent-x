package com.agentx.core.security;

import static org.junit.jupiter.api.Assertions.*;

import com.agentx.api.agent.AgentAction;
import com.agentx.api.context.AgentContext;
import com.agentx.api.security.ApprovalContext;
import com.agentx.api.security.ApprovalRequest;
import com.agentx.api.security.ApprovalResult;
import com.agentx.api.security.Guardrail;
import com.agentx.api.security.GuardrailResult;
import com.agentx.api.security.PermissionDecision;
import com.agentx.api.security.PermissionStatus;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import org.junit.jupiter.api.Test;

public class SecurityAndGuardrailTest {

    @Test
    public void testCompositeGuardrail() {
        Guardrail passGuard = (action, ctx) -> GuardrailResult.pass();
        Guardrail failGuard = (action, ctx) -> GuardrailResult.fail("BlockPolicy", "Malicious content");

        CompositeGuardrail compositePass = new CompositeGuardrail(List.of(passGuard, passGuard));
        AgentAction action = new AgentAction("act-1", "TEST", Map.of());
        AgentContext context = new AgentContext(List.of(), Map.of(), "", Map.of());

        assertTrue(compositePass.validate(action, context).passed());

        CompositeGuardrail compositeFail = new CompositeGuardrail(List.of(passGuard, failGuard));
        GuardrailResult result = compositeFail.validate(action, context);
        assertFalse(result.passed());
        assertEquals("BlockPolicy", result.policyName());
        assertEquals("Malicious content", result.failureReason());
    }

    @Test
    public void testDefaultPermissionManager() {
        DefaultPermissionManager pm = new DefaultPermissionManager();
        pm.grant("safe-tool");
        pm.deny("unsafe-tool", "Not allowed in sandbox");
        pm.requireApproval("sensitive-tool", "Requires human validation");

        AgentContext context = new AgentContext(List.of(), Map.of(), "", Map.of());

        AgentAction safeAction = new AgentAction("a1", "CALL", Map.of("toolName", "safe-tool"));
        PermissionDecision safeDecision = pm.check(safeAction, context);
        assertEquals(PermissionStatus.ALLOW, safeDecision.status());

        AgentAction unsafeAction = new AgentAction("a2", "CALL", Map.of("toolName", "unsafe-tool"));
        PermissionDecision unsafeDecision = pm.check(unsafeAction, context);
        assertEquals(PermissionStatus.DENY, unsafeDecision.status());
        assertEquals("Not allowed in sandbox", unsafeDecision.reason());

        AgentAction sensitiveAction = new AgentAction("a3", "CALL", Map.of("toolName", "sensitive-tool"));
        PermissionDecision sensitiveDecision = pm.check(sensitiveAction, context);
        assertEquals(PermissionStatus.REQUIRE_APPROVAL, sensitiveDecision.status());
    }

    @Test
    public void testSimpleApprovalProviderAutoApprove() {
        SimpleApprovalProvider provider = new SimpleApprovalProvider(true);
        AgentAction action = new AgentAction("a1", "CALL", Map.of());
        ApprovalContext context = new ApprovalContext("exec-1", action, new AgentContext(List.of(), Map.of(), "", Map.of()));

        ApprovalRequest request = provider.request(context);
        ApprovalResult result = provider.waitFor(request);
        assertTrue(result.approved());
        assertEquals("Auto approved", result.reason());
    }

    @Test
    public void testSimpleApprovalProviderManualRespond() {
        SimpleApprovalProvider provider = new SimpleApprovalProvider(false);
        AgentAction action = new AgentAction("a1", "CALL", Map.of());
        ApprovalContext context = new ApprovalContext("exec-1", action, new AgentContext(List.of(), Map.of(), "", Map.of()));

        ApprovalRequest request = provider.request(context);

        // Respond asynchronously after a brief delay
        ForkJoinPool.commonPool().submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            provider.respond(request.id(), true, "Approved by admin");
        });

        ApprovalResult result = provider.waitFor(request);
        assertTrue(result.approved());
        assertEquals("Approved by admin", result.reason());
    }
}
