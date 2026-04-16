# Audit Monitoring Frontend API

Base path: `/api/v1/audit`

Current scope:
- global single account
- platform audit dashboard v2
- full task coverage:
  - `content_automation`
  - `resume_evaluation`
  - `resume_interview`
  - `document_workspace`
- unified token monitoring:
  - task total tokens
  - step total tokens
  - llm call detail tokens

## 1. Dashboard Overview

`GET /dashboard/overview?range=today&taskType=all`

Query params:
- `range`: optional, `today | 7d | 30d`, default `today`
- `taskType`: optional, default `all`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "range": "today",
    "taskType": "all",
    "taskTotal": 42,
    "successTotal": 35,
    "failedTotal": 5,
    "runningTotal": 2,
    "successRate": 83.33,
    "avgDurationMs": 21800,
    "timeoutTotal": 3,
    "degradedTotal": 4,
    "modelCalls": 96,
    "promptTokens": 128430,
    "completionTokens": 93452,
    "totalTokens": 221882,
    "publishSuccessTotal": 6,
    "publishFailedTotal": 1
  }
}
```

Frontend usage:
- render top metric cards
- show token cards with thousands separator
- when `taskType != all`, cards show the selected business line only

## 2. Task Trend

`GET /dashboard/task-trend?days=7&taskType=all`

Query params:
- `days`: optional, default `7`, max `30`
- `taskType`: optional, default `all`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": [
    {
      "date": "2026-04-15",
      "taskTotal": 18,
      "successTotal": 15,
      "failedTotal": 2,
      "runningTotal": 1,
      "promptTokens": 40210,
      "completionTokens": 28600,
      "totalTokens": 68810
    }
  ]
}
```

Frontend usage:
- render task count trend
- render token trend in same chart or a second chart

## 3. Task Type Distribution

`GET /dashboard/task-type?range=7d`

Query params:
- `range`: optional, `today | 7d | 30d`, default `7d`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": [
    {
      "taskType": "content_automation",
      "taskTypeName": "内容自动发布",
      "taskTotal": 10,
      "successTotal": 8,
      "failedTotal": 1,
      "successRate": 80.0,
      "promptTokens": 52000,
      "completionTokens": 40100,
      "totalTokens": 92100
    },
    {
      "taskType": "document_workspace",
      "taskTypeName": "文档知识助手",
      "taskTotal": 16,
      "successTotal": 15,
      "failedTotal": 1,
      "successRate": 93.75,
      "promptTokens": 29100,
      "completionTokens": 18200,
      "totalTokens": 47300
    }
  ]
}
```

Frontend usage:
- render task type distribution
- support switching between task count and token count

## 4. Step Metrics

`GET /dashboard/step-metrics?taskType=content_automation&range=7d`

Query params:
- `taskType`: required for step metrics
- `range`: optional, `today | 7d | 30d`, default `7d`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": [
    {
      "stepName": "draft",
      "stepNameLabel": "生成初稿",
      "stage": "CONTENT_DRAFT",
      "executeTotal": 10,
      "successTotal": 9,
      "failedTotal": 1,
      "timeoutTotal": 1,
      "degradedTotal": 0,
      "avgDurationMs": 65200,
      "successRate": 90.0,
      "llmCallTotal": 10,
      "promptTokens": 16400,
      "completionTokens": 23300,
      "totalTokens": 39700,
      "avgTotalTokens": 3970
    }
  ]
}
```

Frontend usage:
- render node success-rate funnel
- render avg duration ranking
- render token ranking by step

## 5. Model Metrics

`GET /dashboard/model-metrics?range=7d&taskType=all`

Query params:
- `range`: optional, `today | 7d | 30d`, default `7d`
- `taskType`: optional, default `all`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": [
    {
      "clientId": "5301",
      "modelCode": "2007",
      "callTotal": 24,
      "successTotal": 23,
      "failedTotal": 1,
      "avgDurationMs": 38450,
      "promptTokens": 41800,
      "completionTokens": 36510,
      "totalTokens": 78310,
      "avgTotalTokens": 3262
    },
    {
      "clientId": "5201",
      "modelCode": "deepseek-chat",
      "callTotal": 11,
      "successTotal": 11,
      "failedTotal": 0,
      "avgDurationMs": 11900,
      "promptTokens": 9200,
      "completionTokens": 6110,
      "totalTokens": 15310,
      "avgTotalTokens": 1391
    }
  ]
}
```

Frontend usage:
- render model usage table
- support sorting by total tokens, avg duration, failure count

## 6. Publish Channel Metrics

`GET /dashboard/publish-channel?range=7d`

Query params:
- `range`: optional, `today | 7d | 30d`, default `7d`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": [
    {
      "channel": "devto",
      "channelName": "Dev.to",
      "attemptTotal": 5,
      "successTotal": 4,
      "failedTotal": 1,
      "successRate": 80.0,
      "lastStatus": "DRAFT_SAVED",
      "lastMessage": "Dev.to 草稿保存成功",
      "lastExternalUrl": "https://dev.to/dashboard"
    }
  ]
}
```

Frontend usage:
- only show this section when `taskType=all` or `taskType=content_automation`

## 7. Task Mode Metrics For Document Workspace

`GET /dashboard/document-mode?range=7d`

Query params:
- `range`: optional, `today | 7d | 30d`, default `7d`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": [
    {
      "mode": "ask",
      "modeName": "文档提问",
      "taskTotal": 14,
      "successTotal": 13,
      "failedTotal": 1,
      "promptTokens": 12600,
      "completionTokens": 8200,
      "totalTokens": 20800
    },
    {
      "mode": "summary",
      "modeName": "文档摘要",
      "taskTotal": 6,
      "successTotal": 6,
      "failedTotal": 0,
      "promptTokens": 5800,
      "completionTokens": 4100,
      "totalTokens": 9900
    }
  ]
}
```

Frontend usage:
- only show when `taskType=document_workspace`

## 8. Interview Session Metrics

`GET /dashboard/interview-metrics?range=7d`

Query params:
- `range`: optional, `today | 7d | 30d`, default `7d`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "sessionTotal": 9,
    "completedSessionTotal": 7,
    "abortedSessionTotal": 2,
    "roundTotal": 34,
    "avgRoundsPerSession": 3.78,
    "promptTokens": 22400,
    "completionTokens": 17300,
    "totalTokens": 39700
  }
}
```

Frontend usage:
- only show when `taskType=resume_interview`

## 9. Audit Event List

`GET /events?taskType=all&status=FAILED&page=1&pageSize=20`

Query params:
- `taskType`: optional, default `all`
- `bizType`: optional
- `eventType`: optional
- `status`: optional, `SUCCESS | FAILED | RUNNING | DEGRADED`
- `page`: optional, default `1`
- `pageSize`: optional, default `20`, max `100`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "page": 1,
    "pageSize": 20,
    "total": 1,
    "items": [
      {
        "eventId": "audit_001",
        "eventType": "CONTENT_PUBLISH_EXECUTE",
        "eventTypeName": "内容发布执行",
        "bizType": "content_automation",
        "bizId": "21",
        "sessionId": "content-21",
        "executionMode": "STRUCTURED_PLAN_EXECUTE",
        "operatorId": "global",
        "operatorName": "全局账号",
        "status": "FAILED",
        "errorCode": "PUBLISH_FORBIDDEN",
        "errorMessage": "Dev.to 返回 403，当前 API Key 无发布权限或账号状态受限",
        "location": "PublishExecutorNode#apply",
        "metadataJson": "{\"channel\":\"devto\",\"taskId\":21}",
        "createTime": "2026-04-15 22:40:00"
      }
    ]
  }
}
```

Frontend usage:
- render unified audit table for all task types
- clicking a row opens the execution detail drawer if `traceId` is available in `metadataJson`

## 10. Execution Metric Detail

`GET /execution/{traceId}`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "traceId": "trace_001",
    "taskType": "content_automation",
    "taskId": "21",
    "sessionId": "content-21",
    "executionMode": "STRUCTURED_PLAN_EXECUTE",
    "status": "SUCCESS",
    "totalDurationMs": 128000,
    "stepCount": 8,
    "successStepCount": 8,
    "failedStepCount": 0,
    "timeoutCount": 0,
    "retryCount": 0,
    "degradedCount": 0,
    "modelCalls": 6,
    "promptTokens": 30200,
    "completionTokens": 26400,
    "totalTokens": 56600,
    "createTime": "2026-04-15 22:38:00",
    "finishTime": "2026-04-15 22:40:08",
    "steps": [
      {
        "stepNo": 3,
        "stepName": "draft",
        "stepNameLabel": "生成初稿",
        "stage": "CONTENT_DRAFT",
        "clientId": "5301",
        "modelCode": "2007",
        "status": "SUCCESS",
        "durationMs": 65000,
        "retryCount": 0,
        "timeoutFlag": false,
        "degradedFlag": false,
        "promptTokens": 8200,
        "completionTokens": 11600,
        "totalTokens": 19800,
        "llmCallTotal": 1,
        "errorCode": null,
        "errorMessage": null,
        "location": "DraftGeneratorNode#apply"
      }
    ]
  }
}
```

Frontend usage:
- render task summary section
- render stage timeline with durations and token totals
- clicking one step can load llm call detail

## 11. LLM Call Detail List

`GET /execution/{traceId}/llm-calls`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": [
    {
      "callId": "llm_001",
      "traceId": "trace_001",
      "taskType": "content_automation",
      "taskId": "21",
      "sessionId": "content-21",
      "stepName": "draft",
      "stage": "CONTENT_DRAFT",
      "clientId": "5301",
      "modelCode": "2007",
      "status": "SUCCESS",
      "durationMs": 64880,
      "promptTokens": 8200,
      "completionTokens": 11600,
      "totalTokens": 19800,
      "errorCode": null,
      "errorMessage": null,
      "location": "DraftGeneratorNode#apply",
      "createTime": "2026-04-15 22:39:02"
    }
  ]
}
```

Frontend usage:
- render llm call table in execution detail drawer
- support filtering by `stepName`

## 12. Suggested Frontend Layout

Page title:

`审计监控中心`

Top filters:
- time range
- task type
- status

Sections:
1. Overview metric cards
2. Task trend chart
3. Task type distribution
4. Step metrics
5. Model metrics
6. Scene section:
   - publish channel cards for `content_automation`
   - document mode metrics for `document_workspace`
   - interview session metrics for `resume_interview`
7. Audit event table
8. Execution detail drawer

## 13. Current Boundaries

Current user model:
- single global account

Supported task types:
- `content_automation`
- `resume_evaluation`
- `resume_interview`
- `document_workspace`

Current event status:
- `SUCCESS`
- `FAILED`
- `RUNNING`
- `DEGRADED`
