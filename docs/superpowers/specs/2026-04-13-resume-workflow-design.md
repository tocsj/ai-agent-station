# Resume Workflow Design

**Goal**

On top of the existing database-driven AI client assembly and four-step Auto Agent chain, build a minimal end-to-end demo flow for `Resume Knowledge Space -> Resume Evaluation -> Mock Interview`.

**Scope**

This round only delivers one vertical slice:

1. Upload a resume PDF.
2. Parse, split, embed, and store chunks in PGVector.
3. Reuse one Resume Knowledge Space for both resume evaluation and mock interview.
4. Run `1001` for resume evaluation through SSE.
5. Run `1002` for mock interview in a half-closed loop:
   - system proposes 3-5 questions
   - user answers one round
   - system returns feedback and next question through SSE

This round does not build:

- generic document workspace
- heavy multi-agent orchestration
- generalized workflow engine
- production-grade observability dashboard
- content automation publishing flow

## Design Principles

1. Keep the existing dynamic client/model/advisor loading chain unchanged.
2. Keep MySQL config tables as the source of agent/client orchestration.
3. Add a product-scene workflow layer outside the existing `autoAgent` strategy instead of replacing it.
4. Standardize vector metadata now so multi-recall and rerank can be added later without reshaping the business API.
5. Keep the first frontend as a minimal demo surface, not a broad platform shell.

## Architecture

The implementation is split into four layers:

1. Product scene layer
   - Resume upload
   - Resume evaluation
   - Mock interview

2. Resume workflow layer
   - upload and parse orchestration
   - knowledge space lifecycle
   - agent invocation parameter building
   - interview round state management

3. Existing agent runtime layer
   - dynamic DB-driven client loading
   - four-step agent execution chain
   - SSE process streaming

4. Infrastructure layer
   - MySQL for config and business records
   - PostgreSQL PGVector for resume chunk retrieval
   - Redis remains untouched in this round

## Backend Design

### 1. Business Model

Add resume-scene business tables to MySQL instead of overloading config tables:

- `resume_profile`
  - one uploaded resume file and parsed profile root
- `resume_knowledge_space`
  - logical knowledge space record reused by evaluation and interview
- `resume_interview_session`
  - current mock interview session state
- `resume_interview_round`
  - question/answer/feedback per round

These tables are business-facing and must not interfere with `ai_client_*`.

### 2. Vector Metadata Contract

Each chunk written to PGVector must carry normalized metadata:

- `knowledge = resume_knowledge_space`
- `knowledgeSpaceId`
- `resumeId`
- `fileName`
- `docType = resume`
- `chunkIndex`

This keeps the current `RagAnswer` advisor usable and leaves room for future:

- multi-recall merge
- source filtering
- rerank by `2006`
- document-type routing

### 3. API Surface

Add scene-facing endpoints and keep `auto_agent` as an internal execution surface:

- `POST /api/v1/resume/upload`
  - multipart upload
  - parse and store resume chunks
  - return `resumeId` and `knowledgeSpaceId`

- `POST /api/v1/resume/evaluate/stream`
  - input: `resumeId` or `knowledgeSpaceId`
  - internally invoke agent `1001`
  - stream process/result SSE

- `POST /api/v1/resume/interview/start`
  - input: `resumeId` or `knowledgeSpaceId`
  - initialize interview session
  - return first question batch or opening summary

- `POST /api/v1/resume/interview/answer/stream`
  - input: `interviewSessionId`, `questionId`, `answer`
  - internally invoke agent `1002`
  - stream evaluation and next-step suggestion SSE

### 4. Agent Invocation Strategy

Do not change `ExecuteCommandEntity` yet unless strictly necessary for this round.

For the first implementation:

- build a structured business prompt in the workflow layer
- pass it through the existing `message`
- keep `aiAgentId`, `sessionId`, `maxStep`

This avoids widening the existing core runtime prematurely.

If later expansion needs stronger scene context, then a new command object can wrap the current one. That is a second-step change, not required now.

### 5. Retrieval Extension Point

The workflow layer must not call `vectorStore.similaritySearch` directly from controllers.

Create a retrieval abstraction shaped like:

- `retrieveResumeContext`
- internally: recall -> optional merge -> optional rerank -> prompt context build

In this round:

- single recall from PGVector
- no rerank in production path yet

Extension path later:

- add keyword recall
- add BM25/SQL recall
- add rerank by model `2006`
- add score tracing

## Frontend Design

The repo does not currently contain a dedicated React frontend project. To stay aligned with the existing repository and keep scope controlled, this round uses a minimal two-step demo UI built on the existing static frontend/demo surface.

Two-step flow:

### Step 1: Upload and Evaluate

- upload PDF
- show parsing / embedding / storage status
- trigger resume evaluation
- render SSE process blocks and final evaluation report

### Step 2: Mock Interview

- start interview from the evaluated resume
- show generated questions
- let the user answer one round
- show streamed feedback and the next question

This UI is intentionally narrow and demo-oriented. It is not the final product shell.

## Error Handling

Must explicitly handle:

1. upload parse failure
2. embedding write failure
3. PGVector metadata mismatch
4. agent config not loaded for interview clients
5. SSE interruption

The UI should surface step-level failure rather than a generic "request failed".

## Verification Strategy

Minimum verification for this round:

1. upload one PDF and confirm chunks land in PGVector with correct metadata
2. resume evaluation SSE returns process and final report
3. interview start creates a session
4. answering one round returns SSE feedback and next question
5. `5201-5204` are actually loaded by app config

## Future Extension Fit

This design intentionally preserves future expansion:

- new scene agents can be added by adding business workflow routes and MySQL config only
- multi-recall can be introduced behind the retrieval abstraction
- rerank can be added between recall and context build with model `2006`
- document assistant can reuse the same upload/vector/retrieval path with a different scene workflow

## Current Implementation Decision

This round will proceed with:

- workflow layer outside existing Auto Agent chain
- business endpoints for resume scene
- PGVector metadata standardization
- minimal demo UI
- no heavy runtime rewrite
