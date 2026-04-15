# Mock Interview Closed Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a fixed 3-round mock interview workflow that persists rounds, streams per-round evaluation, generates the next question for rounds 1-2, and returns a final report at round 3.

**Architecture:** Keep interview start as a lightweight service call and keep per-answer evaluation inside the existing four-step agent loop. Persist session/round state in MySQL, reuse Resume KS retrieval through the existing RAG advisor, and expose a query API for front-end recovery and replay.

**Tech Stack:** Spring Boot, Spring AI, MySQL, PGVector, SSE, existing dynamic AI client armory, existing auto agent execute strategy.

---

## File Map

### Existing files to modify

- `ai-agent-station-app/src/main/java/com/tkck/app/resume/ResumeWorkflowSchemaInitializer.java`
  - Add missing interview session columns if not present.
- `ai-agent-station-app/src/main/java/com/tkck/app/resume/ResumeWorkflowServiceImpl.java`
  - Central interview session/round orchestration.
- `ai-agent-station-api/src/main/java/com/tkck/api/dto/ResumeInterviewStartResponseDTO.java`
  - Add fields required by front-end if needed.
- `ai-agent-station-trigger/src/main/java/com/tkck/trigger/http/ResumeWorkflowController.java`
  - Add interview detail query endpoint.
- `ai-agent-station-domain/src/main/java/com/tkck/domain/resume/service/IResumeWorkflowService.java`
  - Add interview detail query contract.
- `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/execute/auto/step/Step4LogExecutionSummaryNode.java`
  - Ensure round 1-2 summary yields next question and round 3 yields final report.
- `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/execute/auto/step/Step2PrecisionExecutorNode.java`
  - Tighten executor output constraints for interview rounds.
- `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/execute/auto/step/Step3QualitySupervisorNode.java`
  - Tighten verification rules for next-question/final-report output.

### New files to create

- `ai-agent-station-api/src/main/java/com/tkck/api/dto/ResumeInterviewDetailResponseDTO.java`
  - API response for interview detail page.
- `ai-agent-station-domain/src/main/java/com/tkck/domain/resume/model/entity/ResumeInterviewDetailEntity.java`
  - Domain entity for session + rounds query.
- `ai-agent-station-domain/src/main/java/com/tkck/domain/resume/model/entity/ResumeInterviewRoundEntity.java`
  - Domain entity for a round record.
- `ai-agent-station-app/src/test/java/com/tkck/test/resume/ResumeInterviewWorkflowServiceTest.java`
  - Focused service-level tests for 3-round lifecycle.

### Existing files to test

- `ai-agent-station-app/src/test/java/com/tkck/test/resume/ResumeWorkflowPromptBuilderTest.java`
  - Extend if prompt builder grows.

---

### Task 1: Lock interview schema for 3-round lifecycle

**Files:**
- Modify: `ai-agent-station-app/src/main/java/com/tkck/app/resume/ResumeWorkflowSchemaInitializer.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/resume/ResumeInterviewWorkflowServiceTest.java`

- [ ] **Step 1: Write the failing test**

Write a test that expects interview session records to carry:
- `total_rounds`
- `final_report`
- round rows with `evaluation_content` and `score`

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl ai-agent-station-app -am -Dtest=ResumeInterviewWorkflowServiceTest test
```

Expected: FAIL because schema fields or write logic are missing.

- [ ] **Step 3: Add minimal schema initialization**

Update initializer logic to:
- create missing columns with idempotent SQL
- avoid destructive table recreation

- [ ] **Step 4: Run test to verify schema assumptions pass**

Run:

```bash
mvn -pl ai-agent-station-app -am -Dtest=ResumeInterviewWorkflowServiceTest test
```

Expected: PASS for schema presence assertions.

- [ ] **Step 5: Commit**

```bash
git add ai-agent-station-app/src/main/java/com/tkck/app/resume/ResumeWorkflowSchemaInitializer.java ai-agent-station-app/src/test/java/com/tkck/test/resume/ResumeInterviewWorkflowServiceTest.java
git commit -m "feat: add interview session lifecycle schema"
```

### Task 2: Add interview detail query model

**Files:**
- Create: `ai-agent-station-api/src/main/java/com/tkck/api/dto/ResumeInterviewDetailResponseDTO.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/resume/model/entity/ResumeInterviewDetailEntity.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/resume/model/entity/ResumeInterviewRoundEntity.java`
- Modify: `ai-agent-station-domain/src/main/java/com/tkck/domain/resume/service/IResumeWorkflowService.java`

- [ ] **Step 1: Write the failing compile/test expectation**

Define the target response shape:
- session meta
- `currentRound`
- `totalRounds`
- `status`
- `rounds`
- `finalReport`

- [ ] **Step 2: Run compile or targeted test to confirm missing types**

Run:

```bash
mvn -pl ai-agent-station-app -am -DskipTests compile
```

Expected: FAIL if service/controller references are added before DTO/entity creation.

- [ ] **Step 3: Add DTO and domain entities**

Implement small focused classes only for query/read use.

- [ ] **Step 4: Run compile**

Run:

```bash
mvn -pl ai-agent-station-app -am -DskipTests compile
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-agent-station-api/src/main/java/com/tkck/api/dto/ResumeInterviewDetailResponseDTO.java ai-agent-station-domain/src/main/java/com/tkck/domain/resume/model/entity/ResumeInterviewDetailEntity.java ai-agent-station-domain/src/main/java/com/tkck/domain/resume/model/entity/ResumeInterviewRoundEntity.java ai-agent-station-domain/src/main/java/com/tkck/domain/resume/service/IResumeWorkflowService.java
git commit -m "feat: add interview detail query models"
```

### Task 3: Persist full round results in interview service

**Files:**
- Modify: `ai-agent-station-app/src/main/java/com/tkck/app/resume/ResumeWorkflowServiceImpl.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/resume/ResumeInterviewWorkflowServiceTest.java`

- [ ] **Step 1: Write the failing test**

Cover these service behaviors:
- start interview sets `total_rounds=3`
- round 1 start inserts `ASKED`
- answer round 1 updates answer/status
- later result persistence can store evaluation and score

- [ ] **Step 2: Run the test to verify failure**

Run:

```bash
mvn -pl ai-agent-station-app -am -Dtest=ResumeInterviewWorkflowServiceTest test
```

Expected: FAIL on missing lifecycle persistence.

- [ ] **Step 3: Implement minimal persistence helpers**

Add methods in service for:
- load interview session
- append next round question
- persist round evaluation
- finish session with final report

- [ ] **Step 4: Run the test again**

Run:

```bash
mvn -pl ai-agent-station-app -am -Dtest=ResumeInterviewWorkflowServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-agent-station-app/src/main/java/com/tkck/app/resume/ResumeWorkflowServiceImpl.java ai-agent-station-app/src/test/java/com/tkck/test/resume/ResumeInterviewWorkflowServiceTest.java
git commit -m "feat: persist fixed-round interview state"
```

### Task 4: Add interview detail query endpoint

**Files:**
- Modify: `ai-agent-station-app/src/main/java/com/tkck/app/resume/ResumeWorkflowServiceImpl.java`
- Modify: `ai-agent-station-trigger/src/main/java/com/tkck/trigger/http/ResumeWorkflowController.java`
- Modify: `ai-agent-station-domain/src/main/java/com/tkck/domain/resume/service/IResumeWorkflowService.java`
- Create/Use: `ai-agent-station-api/src/main/java/com/tkck/api/dto/ResumeInterviewDetailResponseDTO.java`

- [ ] **Step 1: Write the failing controller/service test or compile target**

Target endpoint:

```text
GET /api/v1/resume/interview/{interviewSessionId}
```

- [ ] **Step 2: Run compile/test**

Run:

```bash
mvn -pl ai-agent-station-app -am -DskipTests compile
```

Expected: FAIL if endpoint contract is added before implementation.

- [ ] **Step 3: Implement service query + controller mapping**

Return:
- session info
- rounds list
- final report

- [ ] **Step 4: Run compile**

Run:

```bash
mvn -pl ai-agent-station-app -am -DskipTests compile
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-agent-station-app/src/main/java/com/tkck/app/resume/ResumeWorkflowServiceImpl.java ai-agent-station-trigger/src/main/java/com/tkck/trigger/http/ResumeWorkflowController.java ai-agent-station-domain/src/main/java/com/tkck/domain/resume/service/IResumeWorkflowService.java ai-agent-station-api/src/main/java/com/tkck/api/dto/ResumeInterviewDetailResponseDTO.java
git commit -m "feat: add interview detail query endpoint"
```

### Task 5: Constrain round 1-2 output to include next question

**Files:**
- Modify: `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/execute/auto/step/Step2PrecisionExecutorNode.java`
- Modify: `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/execute/auto/step/Step3QualitySupervisorNode.java`
- Modify: `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/execute/auto/step/Step4LogExecutionSummaryNode.java`

- [ ] **Step 1: Write a failing prompt/behavior test or at minimum define expected output contract**

Expected contract:
- round 1-2 summary must contain next question
- round 3 summary must contain final interview report

- [ ] **Step 2: Run the narrow test or compile check**

Use the smallest local verification available for these nodes.

- [ ] **Step 3: Tighten prompts and parsing rules**

Update node prompts so the model is explicitly told:
- fixed total rounds = 3
- if current round < 3, must output `下一轮问题`
- if current round == 3, must output `最终面试总结`

- [ ] **Step 4: Run compile/tests**

Run:

```bash
mvn -pl ai-agent-station-app -am -DskipTests compile
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/execute/auto/step/Step2PrecisionExecutorNode.java ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/execute/auto/step/Step3QualitySupervisorNode.java ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/execute/auto/step/Step4LogExecutionSummaryNode.java
git commit -m "feat: enforce fixed-round interview output contract"
```

### Task 6: Persist next question and final report from streamed interview output

**Files:**
- Modify: `ai-agent-station-app/src/main/java/com/tkck/app/resume/ResumeWorkflowServiceImpl.java`
- Modify: `ai-agent-station-trigger/src/main/java/com/tkck/trigger/http/ResumeWorkflowController.java`

- [ ] **Step 1: Write failing integration expectation**

Expectation:
- round 1/2 stream completion causes next round row creation
- round 3 stream completion stores final report and marks session `FINISHED`

- [ ] **Step 2: Run test or reproduce manually**

Run the targeted test if present; otherwise document manual reproduction steps and current failure.

- [ ] **Step 3: Implement stream-result post-processing**

Add a controlled post-processing path that interprets final summary content and:
- writes evaluation to current round
- creates next round when needed
- completes session when round 3 ends

- [ ] **Step 4: Run verification**

Run:

```bash
mvn -pl ai-agent-station-app -am -DskipTests compile
```

Then manually verify:
- start interview
- answer round 1
- answer round 2
- answer round 3
- query detail endpoint

- [ ] **Step 5: Commit**

```bash
git add ai-agent-station-app/src/main/java/com/tkck/app/resume/ResumeWorkflowServiceImpl.java ai-agent-station-trigger/src/main/java/com/tkck/trigger/http/ResumeWorkflowController.java
git commit -m "feat: persist interview round outputs and final report"
```

### Task 7: Add front-end facing response notes and docs

**Files:**
- Modify: `final_blueprint.md`
- Optionally modify: `docs/superpowers/specs/2026-04-13-mock-interview-closed-loop-design.md`

- [ ] **Step 1: Document final API behavior**

Document:
- fixed 3-round lifecycle
- detail endpoint
- what front-end can rely on after each round

- [ ] **Step 2: Verify docs match implementation**

Compare docs against controller/service behavior.

- [ ] **Step 3: Commit**

```bash
git add final_blueprint.md docs/superpowers/specs/2026-04-13-mock-interview-closed-loop-design.md
git commit -m "docs: describe fixed-round interview closed loop"
```

---

## Verification Checklist

- [ ] Upload resume succeeds and returns `resumeId` + `knowledgeSpaceId`
- [ ] Resume evaluation still works after interview changes
- [ ] Interview start returns `interviewSessionId`, `currentRound=1`, opening question
- [ ] Round 1 answer returns stream + creates round 2
- [ ] Round 2 answer returns stream + creates round 3
- [ ] Round 3 answer returns stream + final report
- [ ] Interview detail endpoint returns full session history
- [ ] Session is marked `FINISHED` at round 3

## Notes for Implementation

1. Do not replace the existing four-step runtime.
2. Do not introduce dynamic-round planning yet.
3. Keep start-interview lightweight.
4. Keep RAG retrieval in the evaluation path only.
5. Avoid storing raw transient SSE fragments as business truth; persist normalized round/session results.
