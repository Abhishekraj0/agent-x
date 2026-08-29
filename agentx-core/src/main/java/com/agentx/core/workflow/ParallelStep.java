package com.agentx.core.workflow;

import com.agentx.api.workflow.WorkflowContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * WorkflowStep that executes child steps in parallel using virtual threads.
 */
public class ParallelStep implements WorkflowStep {

    private final String id;
    private final List<WorkflowStep> steps;

    public ParallelStep(String id, List<WorkflowStep> steps) {
        this.id = id;
        this.steps = steps != null ? steps : List.of();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Map<String, Object> execute(WorkflowContext context) {
        Map<String, Object> mergedOutputs = new HashMap<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<Map<String, Object>>> tasks = steps.stream()
                    .map(step -> (Callable<Map<String, Object>>) () -> step.execute(context))
                    .toList();
            
            List<Future<Map<String, Object>>> futures = executor.invokeAll(tasks);
            for (Future<Map<String, Object>> future : futures) {
                Map<String, Object> subOutput = future.get();
                if (subOutput != null) {
                    mergedOutputs.putAll(subOutput);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Parallel execution failed in step " + id, e);
        }
        context.variables().putAll(mergedOutputs);
        return mergedOutputs;
    }
}
