# Test Inventory and Classification

This document maps all the existing tests in the AgentX codebase, classifying each by type and capability.

## 📋 Test Count Statistics
- Total Tests: **38** tests

| Module | Test Class | Test Method | Type | Capability |
| ------ | ---------- | ----------- | ---- | ---------- |
| agentx-api | ApiTest | testAgentRequestCreation | UNIT | API boundary verification |
| agentx-api | ApiTest | testChatMessageCreation | UNIT | API boundary verification |
| agentx-api | ApiTest | testToolIdFullName | UNIT | API boundary verification |
| agentx-api | ApiTest | testAgentStateInitial | UNIT | API boundary verification |
| agentx-core | ObservabilityTest | testEventBusSubscriptionAndPublishing | UNIT | Event distribution reliability |
| agentx-core | ToolSystemTest | testRegisterAndExecuteTool | UNIT | Registry mapping and invocation |
| agentx-core | ToolSystemTest | testRegistryFindAndResolver | UNIT | Tool query and resolution |
| agentx-core | ModelProvidersTest | testAnthropicModelMetadata | UNIT | Vendor capability metadata |
| agentx-core | ModelProvidersTest | testGeminiModelMetadata | UNIT | Vendor capability metadata |
| agentx-core | ModelProvidersTest | testAzureOpenAIModelMetadata | UNIT | Vendor capability metadata |
| agentx-core | MockChatModelTest | testSyncChat | UNIT | Execution driver mock validation |
| agentx-core | MockChatModelTest | testCustomHandler | UNIT | Custom driver behavior mock |
| agentx-core | MockChatModelTest | testStreamingChat | UNIT | Reactive streaming evaluation |
| agentx-core | ModelProviderIntegrationTest | testOpenAIChatModel | INTEGRATION | HTTP client and OpenAPI payload integration |
| agentx-core | ModelProviderIntegrationTest | testOllamaChatModel | INTEGRATION | HTTP client and Ollama payload integration |
| agentx-core | SecurityAndGuardrailTest | testCompositeGuardrail | UNIT | Security policy combination |
| agentx-core | SecurityAndGuardrailTest | testDefaultPermissionManager | UNIT | Access control decision enforcement |
| agentx-core | SecurityAndGuardrailTest | testSimpleApprovalProviderAutoApprove | UNIT | Approval policy enforcement |
| agentx-core | SecurityAndGuardrailTest | testSimpleApprovalProviderManualRespond | CONCURRENCY | Async human-in-the-loop coordination |
| agentx-core | SecurityAndGuardrailTest | testPromptInjectionGuardrailMessages | SECURITY | Regex prompt injection blocking |
| agentx-core | SecurityAndGuardrailTest | testPromptInjectionGuardrailToolArguments | SECURITY | Tool injection defense |
| agentx-core | AutonomousAgentLoopValidationTest | testGoalEvaluationSemanticCheck | E2E | Semantic goal checks and termination |
| agentx-core | AutonomousAgentLoopValidationTest | testNoInfiniteLoopOnMaxIterations | E2E | Max iteration termination safety |
| agentx-core | AutonomousAgentLoopValidationTest | testDynamicReplanningScenario | E2E | Failure-recovery planning |
| agentx-core | MultiAgentTest | testCoordinatorAndDelegationTool | INTEGRATION | Multi-agent coordination and delegation |
| agentx-core | AgentLoopTest | testAgentExecutionLoop | E2E | Core agent execution loop orchestration |
| agentx-core | RetryAndBudgetTest | testSimpleRetryStrategy | UNIT | Failure handling and backoff retry policy |
| agentx-core | RetryAndBudgetTest | testTokenBudgetManager | UNIT | Budget allocation and exhaustion |
| agentx-core | MemoryAndContextTest | testSlidingWindowContextManager | UNIT | Sliding window context compression |
| agentx-core | MemoryAndContextTest | testInMemoryVectorMemoryStoreSimilarity | INTEGRATION | Similarity-based vector search |
| agentx-core | PluginSystemTest | testPluginLifecycle | INTEGRATION | ServiceLoader dynamic plugin registration |
| agentx-core | WorkflowEngineTest | testSequentialStep | UNIT | Workflows sequential steps execution |
| agentx-core | WorkflowEngineTest | testParallelStep | CONCURRENCY | Parallel workflow steps execution |
| agentx-core | WorkflowEngineTest | testConditionStep | UNIT | Conditional step flow execution |
| agentx-core | WorkflowEngineTest | testDefaultWorkflowExecution | UNIT | Workflow orchestration |
| agentx-mcp | McpClientTest | testMcpToolWrapperExecution | CONTRACT | Model Context Protocol interaction contract |
| agentx-spring | AgentAutoConfigurationTest | testAutoConfigurationBeansRegistered | INTEGRATION | Spring context bean injection |
| agentx-spring | AgentAutoConfigurationTest | testCustomPropertiesAreMapped | INTEGRATION | Spring Boot properties mapping |

### 📈 Verification of Test Count Drift:
- **Phase 1 to Phase 2**: The count went from 29 to 28 because a redundant duplicate test in `RetryAndBudgetTest` was removed and combined.
- **Phase 2 to Phase 3**: The count increased from 28 to 38 (reported previously as 34 but actually verified as 38) due to the addition of:
  - `ModelProvidersTest` (3 tests)
  - `ModelProviderIntegrationTest` (2 tests)
  - `AutonomousAgentLoopValidationTest` (3 tests)
  - `McpClientTest` (1 test)
  - `AgentAutoConfigurationTest` (1 test added for properties mapping)
- **No tests were deleted or regressed without explanation.** All 38 tests are fully operational and verified green.
