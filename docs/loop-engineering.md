# AgentX - Phase 2 Loop Engineering Design

## 1. Abstractions

The new agent loop replaces procedural loop execution with a clean, decoupled state machine. The primary components are:

* **`LoopState`**: Enumerate states representing different phases of loop execution.
* **`LoopTransition`**: Describes valid state-to-state transitions and holds transition logic.
* **`LoopController`**: Executes state transitions within the constraints of termination strategies and budgets.
* **`AgentDecision`**: Sealed interface representing typed decisions (e.g., tool execution, responding, asking user, replanning).
* **`GoalEvaluator`**: Evaluates if the user's objective is completed or if replanning/human interaction is needed.
* **`ActionSelector`**: Decides the next step (either calling a tool, final response, etc.) based on history, tools, and options.
* **`ObservationHandler`**: Integrates tool execution outcomes (observations) back into the agent history/state.
* **`TerminationStrategy`**: Implements global limits (budget, max iterations, cancellation, timeout).

## 2. State Transition Graph

```
                    START
                      |
                      v
                UNDERSTANDING
                      |
                      v
               BUILD CONTEXT
                      |
                      v
               RETRIEVE MEMORY
                      |
                      v
                RESOLVE TOOLS
                      |
                      v
                   PLAN
                      |
                      v
                  DECIDE
                      |
                      v
                VALIDATE
                      |
                      v
               AUTHORIZATION
                 /          \
              DENY          ALLOW
               |              |
               v              v
             FAIL          APPROVAL?
                              |
                       +------+------+
                       |             |
                      YES            NO
                       |             |
                       v             v
                  WAITING        EXECUTE
                  APPROVAL           |
                       |             v
                       +-------- OBSERVE
                                     |
                                     v
                                UPDATE STATE
                                     |
                                     v
                                GOAL EVALUATION
                               /      |       \
                         COMPLETE   CONTINUE   USER
                            |          |         |
                            v          v         v
                         SUCCESS    REPLAN     ASK USER
```

## 3. Transition Rules
* Direct transitions to finished states (`COMPLETED`, `FAILED`, `CANCELLED`, `TIMEOUT`) must validate prerequisites.
* State transitions must verify correctness and fail on invalid transition requests.
