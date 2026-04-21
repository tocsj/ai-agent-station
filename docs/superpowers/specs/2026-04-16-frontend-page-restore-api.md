# Frontend Page Restore API

Base path:
- content automation: `/api/v1/content`
- document workspace: `/api/v1/document`
- resume workflow: `/api/v1/resume`

Current scope:
- global single account
- all product pages can recover after route switching or browser refresh
- backend state is the source of truth
- frontend local state is only an acceleration hint

## 1. Restore Rules For All Pages

Frontend must not rely on component memory. Route switching will unmount page state.

General restore flow:
1. enter page
2. call that page's active/recent API
3. hydrate page from backend response
4. if a task is running, show running state and poll detail APIs every 2-5 seconds
5. do not recreate tasks during restore
6. do not reconnect SSE automatically unless user explicitly clicks continue/retry
7. every new running task must use a unique `sessionId`

Suggested `sessionId` format:

```ts
`${scene}-${bizId}-${Date.now()}`
```

Examples:
- `content-21-1776310000000`
- `resume-eval-9-1776310000000`
- `resume-interview-12-1776310000000`

## 2. Content Automation Restore

This section preserves the previous content automation restore contract.

### Query Active Content Task

`GET /api/v1/content/task/active`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "taskId": 21,
    "taskCode": "ct_021",
    "executionMode": "STRUCTURED_PLAN_EXECUTE",
    "topic": "企业级 AI Agent 编排平台",
    "platform": "Dev.to",
    "style": "专业",
    "keywords": "AI Agent,Java,DDD",
    "channel": "devto",
    "status": "RUNNING",
    "currentStep": "draft",
    "title": "标题",
    "outlineText": "大纲",
    "draftContent": "初稿",
    "finalContent": null,
    "complianceResult": null,
    "publishStatus": null,
    "publishExternalId": null,
    "publishExternalUrl": null,
    "summaryText": null,
    "createTime": "2026-04-16 09:00:00.0",
    "updateTime": "2026-04-16 09:02:00.0"
  }
}
```

When `data` is not null, frontend should also call:
- `GET /api/v1/content/task/{taskId}`
- `GET /api/v1/content/task/{taskId}/steps`

Frontend rules:
- do not call `/task/create` during restore
- `status=RUNNING` means poll detail and steps
- SSE event payload is incremental only; final truth is detail + steps

## 3. Document Workspace Restore

### Query Active Workspace

`GET /api/v1/document/workspace/active`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "workspaceId": "dws_xxx",
    "workspaceName": "Java 架构与设计空间",
    "description": "用于文档检索、摘要、追问和问答",
    "status": "1",
    "documentCount": 1,
    "documents": [
      {
        "docId": "doc_xxx",
        "fileName": "test.txt",
        "fileType": "txt",
        "fileSize": 1024,
        "parseStatus": "COMPLETED",
        "chunkCount": 3,
        "vectorStatus": "COMPLETED"
      }
    ]
  }
}
```

If there is no workspace:

```json
{
  "code": "0000",
  "info": "成功",
  "data": null
}
```

### Query Recent Document Tasks

`GET /api/v1/document/task/recent?workspaceId=dws_xxx&limit=10`

Query params:
- `workspaceId`: optional. If omitted, returns latest tasks across document workspaces.
- `limit`: optional, default `10`, max `50`.

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": [
    {
      "taskId": 1001,
      "workspaceId": "dws_xxx",
      "docId": "doc_xxx",
      "mode": "ask",
      "question": "这份文档的核心观点是什么？",
      "answer": "回答内容",
      "rewrittenQuery": "这份文档的核心观点是什么？",
      "retrievalScope": "workspace:dws_xxx,doc:doc_xxx,vectorTable=document_vector_store",
      "finalContext": "最终上下文",
      "retrievedChunks": ["chunk text"],
      "retrievedChunkDetails": [
        {
          "workspaceId": "dws_xxx",
          "docId": "doc_xxx",
          "fileName": "test.txt",
          "chunkIndex": "1",
          "preview": "片段预览"
        }
      ],
      "status": "SUCCESS",
      "errorMessage": null,
      "createTime": "2026-04-16 09:05:00.0"
    }
  ]
}
```

Frontend restore flow:
1. call `/workspace/active`
2. hydrate workspace and document list
3. call `/task/recent?workspaceId={workspaceId}&limit=10`
4. use the first task to restore center result and right-side RAG panel

## 4. Resume Evaluation Restore

### Query Recent Resume Profiles

`GET /api/v1/resume/profile/recent?limit=20`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": [
    {
      "resumeId": 7,
      "knowledgeSpaceId": 8,
      "knowledgeTag": "resume",
      "fileName": "candidate.pdf",
      "chunkCount": 12,
      "createTime": "2026-04-16 09:10:00.0",
      "updateTime": "2026-04-16 09:10:00.0"
    }
  ]
}
```

### Query Active Resume Evaluation

`GET /api/v1/resume/evaluation/active`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "taskId": 31,
    "resumeId": 7,
    "knowledgeSpaceId": 8,
    "sessionId": "resume-eval-8-1776310000000",
    "question": "请评估这份简历",
    "status": "SUCCESS",
    "report": "评估报告",
    "traceId": "trace_xxx",
    "errorMessage": null,
    "createTime": "2026-04-16 09:11:00.0",
    "updateTime": "2026-04-16 09:12:00.0"
  }
}
```

### Query Recent Resume Evaluations

`GET /api/v1/resume/evaluation/recent?limit=20`

Frontend usage:
- enter resume evaluation page
- call `/profile/recent`
- call `/evaluation/active`
- if active status is `RUNNING`, show running state and poll `/evaluation/active`
- do not call `/evaluate/stream` during restore

## 5. Mock Interview Restore

### Query Active Interview Session

`GET /api/v1/resume/interview/active`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "interviewSessionId": 12,
    "resumeId": 7,
    "knowledgeSpaceId": 8,
    "sessionCode": "interview-xxx",
    "currentRound": 2,
    "totalRounds": 3,
    "status": "IN_PROGRESS",
    "openingQuestions": "开场问题",
    "finalReport": null,
    "rounds": [
      {
        "roundNo": 1,
        "questionContent": "问题",
        "answerContent": "回答",
        "feedbackContent": "反馈",
        "strengths": "优点",
        "weaknesses": "不足",
        "resumeEvidence": "简历依据",
        "followUpIntent": "追问意图",
        "nextQuestion": "下一题",
        "score": "8/10",
        "finished": false,
        "status": "EVALUATED"
      }
    ]
  }
}
```

Frontend usage:
- enter mock interview page
- call `/interview/active`
- if `data != null`, hydrate current interview state directly
- optional: call existing `/interview/{interviewSessionId}` after selecting a historical session

## 6. Audit Dashboard Restore

Audit dashboard is query-based and does not need an active task API.

Frontend should persist filters:
- `range`
- `taskType`
- `status`

On page enter, re-call:
- `GET /api/v1/audit/dashboard/overview`
- `GET /api/v1/audit/dashboard/task-trend`
- `GET /api/v1/audit/dashboard/task-type`
- `GET /api/v1/audit/events`

## 7. Frontend State Boundaries

Recommended global state keys:

```ts
type RestoreState = {
  activeContentTaskId?: number
  activeDocumentWorkspaceId?: string
  activeDocumentTaskId?: number
  activeResumeId?: number
  activeResumeEvaluationTaskId?: number
  activeInterviewSessionId?: number
  auditFilters?: {
    range: 'today' | '7d' | '30d'
    taskType: string
    status?: string
  }
}
```

Rules:
- global state can speed up restore, but backend remains authoritative
- never reuse one `sessionId` for two running tasks
- if the same page supports history selection, selecting history only calls detail APIs and must not create new tasks
