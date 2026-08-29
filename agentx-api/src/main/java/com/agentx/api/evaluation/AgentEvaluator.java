package com.agentx.api.evaluation;

import com.agentx.api.agent.AgentExecution;

/**
 * Interface responsible for assessing an agent execution session for correctness, safety, or efficiency.
 */
public interface AgentEvaluator {

    /**
     * Evaluates the execution against a criteria context.
     *
     * @param execution the agent execution log
     * @param context   the evaluation context/ground truth
     * @return the evaluation result
     */
    EvaluationResult evaluate(AgentExecution execution, EvaluationContext context);
}
