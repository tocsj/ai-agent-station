# Runtime 2.0 And Content Automation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Runtime 2.0 execution modes and deliver the first content automation workflow with mock draft publishing.

**Architecture:** Keep the current platform intact, add a dispatcher-based runtime layer, route existing auto-agent flows through structured mode, and add a new content workflow under the same structured execution family. Document workspace remains single-shot.

**Tech Stack:** Spring Boot, Spring AI, PGVector, MySQL, SSE, JUnit 4, Mockito

---

### Task 1: Runtime mode model and dispatcher

**Files:**
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/model/valobj/ExecutionMode.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/runtime/AgentRuntimeDispatcher.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/runtime/ExecutionHandler.java`
- Modify: `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/model/entity/ExecuteCommandEntity.java`
- Modify: `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/execute/auto/AutoAgentExecuteStrategy.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/runtime/AgentRuntimeDispatcherTest.java`

- [ ] Write failing dispatcher tests
- [ ] Run tests and confirm failure
- [ ] Add execution mode enum and dispatcher
- [ ] Delegate `AutoAgentExecuteStrategy` to dispatcher
- [ ] Run dispatcher tests

### Task 2: Structured workflow handler with legacy compatibility

**Files:**
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/runtime/structured/StructuredWorkflowExecutor.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/runtime/structured/StructuredPlanExecuteHandler.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/runtime/structured/LegacyAutoAgentWorkflowExecutor.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/runtime/StructuredPlanExecuteHandlerTest.java`

- [ ] Write failing tests for task-type based structured routing
- [ ] Run tests and confirm failure
- [ ] Add structured handler and legacy workflow adapter
- [ ] Run structured handler tests

### Task 3: Single-shot handler skeleton

**Files:**
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/runtime/singleshot/SingleShotExecutionHandler.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/runtime/SingleShotExecutionHandlerTest.java`

- [ ] Write failing test for single-shot handler support detection
- [ ] Run test and confirm failure
- [ ] Implement minimal single-shot handler
- [ ] Run test and confirm pass

### Task 4: Content task schema and domain model

**Files:**
- Modify: `ai-agent-station-app/src/main/java/com/tkck/app/resume/ResumeWorkflowSchemaInitializer.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/model/entity/ContentTaskEntity.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/model/entity/ContentTaskStepEntity.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/model/entity/ContentCreateCommandEntity.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/service/IContentAutomationService.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/content/ContentTaskSchemaContractTest.java`

- [ ] Write failing schema/domain contract test
- [ ] Run test and confirm failure
- [ ] Add schema and domain entities
- [ ] Run test and confirm pass

### Task 5: Content application service

**Files:**
- Create: `ai-agent-station-app/src/main/java/com/tkck/app/content/ContentAutomationServiceImpl.java`
- Create: `ai-agent-station-app/src/main/java/com/tkck/app/content/ContentPromptBuilder.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/content/ContentAutomationServiceTest.java`

- [ ] Write failing tests for task create/detail/steps
- [ ] Run tests and confirm failure
- [ ] Implement minimal content application service
- [ ] Run tests and confirm pass

### Task 6: Content workflow nodes and executor

**Files:**
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/service/workflow/ContentWorkflowContext.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/service/workflow/AbstractContentWorkflowNode.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/service/workflow/TopicPlannerNode.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/service/workflow/OutlineGeneratorNode.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/service/workflow/DraftGeneratorNode.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/service/workflow/PolishVerifierNode.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/service/workflow/ComplianceReviewerNode.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/service/workflow/PublishPlannerNode.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/service/workflow/PublishExecutorNode.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/service/workflow/PublishSummarizerNode.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/service/workflow/ContentAutomationWorkflowExecutor.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/content/ContentAutomationWorkflowExecutorTest.java`

- [ ] Write failing workflow test covering all stages
- [ ] Run test and confirm failure
- [ ] Implement nodes with minimal stable prompts and chaining
- [ ] Run tests and confirm pass

### Task 7: Mock publish adapter

**Files:**
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/model/entity/PublishCommandEntity.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/model/entity/PublishResultEntity.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/content/service/publish/IPublishAdapter.java`
- Create: `ai-agent-station-app/src/main/java/com/tkck/app/content/publish/MockPublishAdapter.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/content/MockPublishAdapterTest.java`

- [ ] Write failing test for stable draft publish output
- [ ] Run test and confirm failure
- [ ] Implement mock publish adapter
- [ ] Run tests and confirm pass

### Task 8: Content APIs and SSE contract

**Files:**
- Create: `ai-agent-station-api/src/main/java/com/tkck/api/dto/ContentTaskCreateRequestDTO.java`
- Create: `ai-agent-station-api/src/main/java/com/tkck/api/dto/ContentTaskCreateResponseDTO.java`
- Create: `ai-agent-station-api/src/main/java/com/tkck/api/dto/ContentTaskExecuteRequestDTO.java`
- Create: `ai-agent-station-api/src/main/java/com/tkck/api/dto/ContentTaskDetailResponseDTO.java`
- Create: `ai-agent-station-trigger/src/main/java/com/tkck/trigger/http/ContentAutomationController.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/content/ContentAutomationControllerContractTest.java`

- [ ] Write failing controller contract test
- [ ] Run test and confirm failure
- [ ] Implement create/detail/steps/execute-stream APIs
- [ ] Run tests and confirm pass

### Task 9: End-to-end verification

**Files:**
- Modify: `docs/superpowers/specs/2026-04-15-runtime-2-content-automation-design.md`

- [ ] Run focused runtime tests
- [ ] Run focused content tests
- [ ] Run combined suite for runtime + content + existing document tests
- [ ] Update spec if behavior differs from plan
