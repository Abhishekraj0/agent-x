package com.agentx.core.workflow;

import static org.junit.jupiter.api.Assertions.*;

import com.agentx.api.workflow.WorkflowContext;
import com.agentx.api.workflow.WorkflowDefinition;
import com.agentx.api.workflow.WorkflowResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

public class WorkflowEngineTest {

    @Test
    public void testSequentialStep() {
        WorkflowStep step1 = new WorkflowStep() {
            @Override
            public String id() { return "step1"; }
            @Override
            public Map<String, Object> execute(WorkflowContext context) {
                return Map.of("key1", "value1");
            }
        };

        WorkflowStep step2 = new WorkflowStep() {
            @Override
            public String id() { return "step2"; }
            @Override
            public Map<String, Object> execute(WorkflowContext context) {
                String prevVal = (String) context.variables().get("key1");
                return Map.of("key2", prevVal + "-value2");
            }
        };

        SequentialStep seq = new SequentialStep("seq-1", List.of(step1, step2));
        WorkflowContext context = new WorkflowContext("w-1", Map.of(), new ConcurrentHashMap<>());

        Map<String, Object> output = seq.execute(context);
        assertEquals("value1", output.get("key1"));
        assertEquals("value1-value2", output.get("key2"));
    }

    @Test
    public void testParallelStep() {
        WorkflowStep step1 = new WorkflowStep() {
            @Override
            public String id() { return "step1"; }
            @Override
            public Map<String, Object> execute(WorkflowContext context) {
                try { Thread.sleep(50); } catch (Exception e) {}
                return Map.of("par1", "val1");
            }
        };

        WorkflowStep step2 = new WorkflowStep() {
            @Override
            public String id() { return "step2"; }
            @Override
            public Map<String, Object> execute(WorkflowContext context) {
                try { Thread.sleep(50); } catch (Exception e) {}
                return Map.of("par2", "val2");
            }
        };

        ParallelStep par = new ParallelStep("par-1", List.of(step1, step2));
        WorkflowContext context = new WorkflowContext("w-2", Map.of(), new ConcurrentHashMap<>());

        Map<String, Object> output = par.execute(context);
        assertEquals("val1", output.get("par1"));
        assertEquals("val2", output.get("par2"));
    }

    @Test
    public void testConditionStep() {
        WorkflowStep thenStep = context -> Map.of("branch", "then");
        WorkflowStep elseStep = context -> Map.of("branch", "else");

        ConditionStep condTrue = new ConditionStep("cond-1", 
                context -> "true".equals(context.input().get("flag")),
                thenStep,
                elseStep
        );

        WorkflowContext contextTrue = new WorkflowContext("w-3", Map.of("flag", "true"), new ConcurrentHashMap<>());
        Map<String, Object> outTrue = condTrue.execute(contextTrue);
        assertEquals("then", outTrue.get("branch"));

        WorkflowContext contextFalse = new WorkflowContext("w-3", Map.of("flag", "false"), new ConcurrentHashMap<>());
        Map<String, Object> outFalse = condTrue.execute(contextFalse);
        assertEquals("else", outFalse.get("branch"));
    }

    @Test
    public void testDefaultWorkflowExecution() {
        WorkflowStep root = context -> Map.of("result", "ok");
        WorkflowDefinition definition = new WorkflowDefinition("wf-id", "Test Workflow", List.of("root"), Map.of());
        DefaultWorkflow wf = new DefaultWorkflow(definition, root);

        WorkflowContext context = new WorkflowContext("exec-wf", Map.of(), new ConcurrentHashMap<>());
        WorkflowResult result = wf.execute(context);

        assertTrue(result.success());
        assertEquals("ok", result.output().get("result"));
        assertNull(result.error());
    }
}
