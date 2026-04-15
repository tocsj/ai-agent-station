# Resume Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first end-to-end resume workflow: upload PDF resume, store Resume Knowledge Space in PGVector, run resume evaluation, and run one-round mock interview.

**Architecture:** Keep the existing DB-driven dynamic AI client assembly and four-step Auto Agent execution chain. Add a thin workflow layer for resume-scene orchestration, a minimal business data model, and a minimal demo UI on top of scene-facing APIs.

**Tech Stack:** Spring Boot, Spring AI, MySQL, PostgreSQL PGVector, SSE, static HTML/JS demo UI

---

### Task 1: Align Runtime Configuration

**Files:**
- Modify: `ai-agent-station-app/src/main/resources/application-dev.yml`
- Modify: `ai-agent-station-app/src/main/java/com/tkck/config/AiAgentConfig.java`

- [ ] Add `5201,5202,5203,5204` to runtime auto-config client IDs.
- [ ] Align PGVector datasource/database settings with the current local environment.
- [ ] Align embedding model settings with the retained MySQL model plan.
- [ ] Verify app config reflects the current database and vector store assumptions.

### Task 2: Add Business Persistence for Resume Workflow

**Files:**
- Create: `ai-agent-station-infrastructure/src/main/java/com/tkck/infrastructure/dao/po/ResumeProfile.java`
- Create: `ai-agent-station-infrastructure/src/main/java/com/tkck/infrastructure/dao/po/ResumeKnowledgeSpace.java`
- Create: `ai-agent-station-infrastructure/src/main/java/com/tkck/infrastructure/dao/po/ResumeInterviewSession.java`
- Create: `ai-agent-station-infrastructure/src/main/java/com/tkck/infrastructure/dao/po/ResumeInterviewRound.java`
- Create: matching DAO interfaces and mybatis mapper XML files
- Create: SQL DDL file for new business tables

- [ ] Write schema for resume business entities.
- [ ] Add DAO PO classes and mapper XML.
- [ ] Add minimal CRUD needed for upload/evaluate/interview flow.
- [ ] Verify schema can be applied locally.

### Task 3: Build Resume Upload and Knowledge Space Service

**Files:**
- Create: resume workflow service classes in domain/trigger/infrastructure as needed
- Reuse: `TokenTextSplitter`, `vectorStore`

- [ ] Write failing tests for upload parsing and metadata shaping.
- [ ] Implement PDF parsing using existing Spring AI/Tika-capable path already present in tests.
- [ ] Split documents and write chunks into PGVector with normalized metadata.
- [ ] Persist `resume_profile` and `resume_knowledge_space`.
- [ ] Verify one uploaded resume produces chunk rows with expected metadata.

### Task 4: Add Resume Scene APIs

**Files:**
- Modify: `ai-agent-station-trigger/src/main/java/com/tkck/trigger/http/AiAgentController.java` or add a dedicated resume controller
- Create: request/response DTOs in api/types modules as appropriate

- [ ] Add `resume/upload`.
- [ ] Add `resume/evaluate/stream`.
- [ ] Add `resume/interview/start`.
- [ ] Add `resume/interview/answer/stream`.
- [ ] Keep `auto_agent` unchanged and treat it as internal runtime support.

### Task 5: Wire Resume Evaluation Workflow

**Files:**
- Create/modify workflow orchestrator classes
- Reuse existing `autoAgentExecuteStrategy`

- [ ] Write failing tests for evaluation request construction.
- [ ] Build business prompt composition from resume knowledge context.
- [ ] Invoke agent `1001` using existing runtime path.
- [ ] Stream SSE result back through resume-scene endpoint.
- [ ] Verify final report is returned and intermediate events are visible.

### Task 6: Wire Mock Interview Workflow

**Files:**
- Create/modify interview workflow service classes

- [ ] Write failing tests for interview session creation and one-round answer flow.
- [ ] Implement `interview/start` session bootstrap.
- [ ] Implement one-round answer submission.
- [ ] Invoke agent `1002` and stream feedback plus next question.
- [ ] Persist interview rounds for later extension.

### Task 7: Build Minimal Two-Step Demo UI

**Files:**
- Modify existing static demo page under `docs/dev-ops/nginx/html/`

- [ ] Add step 1 upload/evaluation UI.
- [ ] Add step 2 interview UI.
- [ ] Add SSE event rendering for process and final outputs.
- [ ] Keep layout minimal and focused on the demo flow.

### Task 8: End-to-End Verification

**Files:**
- Add or update focused tests
- Run app locally

- [ ] Verify upload -> PGVector write.
- [ ] Verify evaluation SSE.
- [ ] Verify interview start and one-round answer.
- [ ] Verify Chinese MySQL-configured clients are loaded.
- [ ] Record any remaining gaps explicitly.
