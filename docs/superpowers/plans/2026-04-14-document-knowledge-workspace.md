# Document Knowledge Workspace Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first working Document Knowledge Workspace with multi-document upload, vectorization, workspace-scoped Q&A, and optional summary/followup/quiz actions.

**Architecture:** Reuse the existing `app / domain / trigger` split, keep upload/parse/vectorize logic in `app`, expose HTTP/SSE adapters in `trigger`, and model workspace/document/task contracts in `domain`. Default document tasks use single-call LLM + RAG instead of the four-step runtime, while reusing the same PGVector and resilience base.

**Tech Stack:** Spring Boot, Spring AI, Tika, PostgreSQL PGVector, MySQL, SSE, JUnit 4, Maven

---

### Task 1: Add Document Workspace Schema

**Files:**
- Modify: `ai-agent-station-app/src/main/java/com/tkck/app/resume/ResumeWorkflowSchemaInitializer.java`
- Modify: `ai-agent-station-app/src/main/java/com/tkck/config/AiAgentConfig.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/document/DocumentWorkspaceMetadataSupportTest.java`

- [ ] **Step 1: Write the failing test**

Create a metadata support test that asserts document metadata contains `spaceId`, `docId`, `fileName`, `chunkIndex`, `docType=document`, `knowledge=document_knowledge_space`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl ai-agent-station-app -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DocumentWorkspaceMetadataSupportTest" test`

Expected: FAIL because document metadata support does not exist.

- [ ] **Step 3: Write minimal implementation**

Add:
- document vector table constant
- MySQL tables:
  - `ai_knowledge_space`
  - `ai_knowledge_document`
  - `ai_knowledge_chunk`
- PostgreSQL vector table:
  - `document_vector_store`
- metadata support helper for document chunks

- [ ] **Step 4: Run test to verify it passes**

Run the same command and confirm PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-agent-station-app/src/main/java/com/tkck/app/resume/ResumeWorkflowSchemaInitializer.java ai-agent-station-app/src/main/java/com/tkck/config/AiAgentConfig.java ai-agent-station-app/src/test/java/com/tkck/test/document/DocumentWorkspaceMetadataSupportTest.java
git commit -m "feat: add document workspace schema"
```

### Task 2: Add Domain and API Contracts

**Files:**
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/document/model/entity/DocumentWorkspaceEntity.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/document/model/entity/DocumentWorkspaceDetailEntity.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/document/model/entity/DocumentFileEntity.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/document/model/entity/DocumentTaskResultEntity.java`
- Create: `ai-agent-station-domain/src/main/java/com/tkck/domain/document/service/IDocumentWorkspaceService.java`
- Create: `ai-agent-station-api/src/main/java/com/tkck/api/dto/DocumentWorkspaceCreateRequestDTO.java`
- Create: `ai-agent-station-api/src/main/java/com/tkck/api/dto/DocumentWorkspaceCreateResponseDTO.java`
- Create: `ai-agent-station-api/src/main/java/com/tkck/api/dto/DocumentUploadResponseDTO.java`
- Create: `ai-agent-station-api/src/main/java/com/tkck/api/dto/DocumentAskRequestDTO.java`
- Create: `ai-agent-station-api/src/main/java/com/tkck/api/dto/DocumentSummaryRequestDTO.java`
- Create: `ai-agent-station-api/src/main/java/com/tkck/api/dto/DocumentFollowupRequestDTO.java`
- Create: `ai-agent-station-api/src/main/java/com/tkck/api/dto/DocumentQuizRequestDTO.java`
- Create: `ai-agent-station-api/src/main/java/com/tkck/api/dto/DocumentWorkspaceDetailResponseDTO.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/document/DocumentPromptBuilderTest.java`

- [ ] **Step 1: Write the failing test**

Add prompt builder tests for:
- ask prompt includes workspace/doc filter context
- summary prompt includes mode
- followup prompt includes perspective
- quiz prompt includes question count and type

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl ai-agent-station-app -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DocumentPromptBuilderTest" test`

Expected: FAIL because prompt builder does not exist.

- [ ] **Step 3: Write minimal implementation**

Create document entities, DTOs, and a prompt builder that supports the four document task types.

- [ ] **Step 4: Run test to verify it passes**

Run the same command and confirm PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-agent-station-domain/src/main/java/com/tkck/domain/document ai-agent-station-api/src/main/java/com/tkck/api/dto ai-agent-station-app/src/test/java/com/tkck/test/document/DocumentPromptBuilderTest.java
git commit -m "feat: add document workspace contracts"
```

### Task 3: Implement Workspace Create and Upload Pipeline

**Files:**
- Create: `ai-agent-station-app/src/main/java/com/tkck/app/document/DocumentWorkspaceMetadataSupport.java`
- Create: `ai-agent-station-app/src/main/java/com/tkck/app/document/DocumentWorkspacePromptBuilder.java`
- Create: `ai-agent-station-app/src/main/java/com/tkck/app/document/DocumentWorkspaceServiceImpl.java`
- Modify: `ai-agent-station-app/src/main/java/com/tkck/config/AiAgentConfig.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/document/DocumentWorkspaceServiceUploadTest.java`

- [ ] **Step 1: Write the failing test**

Add a service test with mocked `JdbcTemplate`/`VectorStore` that expects upload to:
- validate file type
- create workspace if requested separately
- create document row
- split into chunks
- attach metadata
- call document vector store

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl ai-agent-station-app -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DocumentWorkspaceServiceUploadTest" test`

Expected: FAIL because service does not exist.

- [ ] **Step 3: Write minimal implementation**

Implement:
- create workspace
- upload document to workspace
- parse file via Tika
- split content
- write MySQL records
- write PGVector records

- [ ] **Step 4: Run test to verify it passes**

Run the same command and confirm PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-agent-station-app/src/main/java/com/tkck/app/document ai-agent-station-app/src/test/java/com/tkck/test/document/DocumentWorkspaceServiceUploadTest.java ai-agent-station-app/src/main/java/com/tkck/config/AiAgentConfig.java
git commit -m "feat: add document upload pipeline"
```

### Task 4: Implement Workspace Query and Default Ask Flow

**Files:**
- Modify: `ai-agent-station-app/src/main/java/com/tkck/app/document/DocumentWorkspaceServiceImpl.java`
- Create: `ai-agent-station-app/src/main/java/com/tkck/app/document/DocumentTaskMode.java`
- Create: `ai-agent-station-app/src/main/java/com/tkck/app/document/DocumentTaskCommand.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/document/DocumentWorkspaceAskServiceTest.java`

- [ ] **Step 1: Write the failing test**

Add tests that assert:
- ask flow builds workspace-scoped query
- optional `docId` narrows filter
- ask flow returns retrieval payload and final answer content

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl ai-agent-station-app -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DocumentWorkspaceAskServiceTest" test`

Expected: FAIL because ask service behavior is missing.

- [ ] **Step 3: Write minimal implementation**

Implement:
- workspace detail query
- default ask task
- workspace filter / doc filter
- retrieval result assembly
- single-call LLM answer

- [ ] **Step 4: Run test to verify it passes**

Run the same command and confirm PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-agent-station-app/src/main/java/com/tkck/app/document ai-agent-station-app/src/test/java/com/tkck/test/document/DocumentWorkspaceAskServiceTest.java
git commit -m "feat: add document workspace ask flow"
```

### Task 5: Add Summary, Followup, and Quiz Task Modes

**Files:**
- Modify: `ai-agent-station-app/src/main/java/com/tkck/app/document/DocumentWorkspaceServiceImpl.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/document/DocumentWorkspaceTaskModeTest.java`

- [ ] **Step 1: Write the failing test**

Add tests that assert:
- summary mode uses summary prompt
- followup mode uses perspective prompt
- quiz mode uses count/type prompt

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl ai-agent-station-app -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DocumentWorkspaceTaskModeTest" test`

Expected: FAIL because extra task modes are not implemented.

- [ ] **Step 3: Write minimal implementation**

Implement the three optional task modes with the same retrieval pipeline and different prompt construction.

- [ ] **Step 4: Run test to verify it passes**

Run the same command and confirm PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-agent-station-app/src/main/java/com/tkck/app/document ai-agent-station-app/src/test/java/com/tkck/test/document/DocumentWorkspaceTaskModeTest.java
git commit -m "feat: add document summary followup and quiz tasks"
```

### Task 6: Add Trigger Endpoints

**Files:**
- Create: `ai-agent-station-trigger/src/main/java/com/tkck/trigger/http/DocumentWorkspaceController.java`
- Modify: `ai-agent-station-domain/src/main/java/com/tkck/domain/document/service/IDocumentWorkspaceService.java`
- Test: `ai-agent-station-app/src/test/java/com/tkck/test/document/DocumentWorkspaceControllerContractTest.java`

- [ ] **Step 1: Write the failing test**

Add controller contract tests that assert the HTTP payload shape for:
- workspace/create
- upload
- workspace detail
- summary/followup/quiz

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl ai-agent-station-app -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DocumentWorkspaceControllerContractTest" test`

Expected: FAIL because controller does not exist.

- [ ] **Step 3: Write minimal implementation**

Add:
- `POST /api/v1/document/workspace/create`
- `POST /api/v1/document/upload`
- `GET /api/v1/document/workspace/{workspaceId}`
- `POST /api/v1/document/ask/stream`
- `POST /api/v1/document/summary`
- `POST /api/v1/document/followup`
- `POST /api/v1/document/quiz`

- [ ] **Step 4: Run test to verify it passes**

Run the same command and confirm PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-agent-station-trigger/src/main/java/com/tkck/trigger/http/DocumentWorkspaceController.java ai-agent-station-domain/src/main/java/com/tkck/domain/document/service/IDocumentWorkspaceService.java ai-agent-station-app/src/test/java/com/tkck/test/document/DocumentWorkspaceControllerContractTest.java
git commit -m "feat: add document workspace endpoints"
```

### Task 7: End-to-End Verification

**Files:**
- Modify if needed: `docs/superpowers/specs/2026-04-14-document-knowledge-workspace-design.md`
- Modify if needed: `final_blueprint.md`

- [ ] **Step 1: Run focused document tests**

Run:

```bash
mvn -pl ai-agent-station-app -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DocumentWorkspaceMetadataSupportTest,DocumentPromptBuilderTest,DocumentWorkspaceServiceUploadTest,DocumentWorkspaceAskServiceTest,DocumentWorkspaceTaskModeTest,DocumentWorkspaceControllerContractTest" test
```

Expected: PASS

- [ ] **Step 2: Run compile verification**

Run:

```bash
mvn -pl ai-agent-station-app -am -DskipTests compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Smoke test existing resume/interview contracts**

Run:

```bash
mvn -pl ai-agent-station-app -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ResumeInterviewStructuredResultParserTest,ResumeWorkflowPromptBuilderTest,ResumeWorkflowPromptBuilderStructuredInterviewTest" test
```

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/specs/2026-04-14-document-knowledge-workspace-design.md final_blueprint.md
git commit -m "docs: finalize document workspace first version"
```
