# Workbench Dashboard Frontend API

Base path: `/api/v1/workbench`

Current scope:
- homepage dashboard only
- summary navigation, not full audit detail
- execution detail still reuses existing audit APIs

## 1. Dashboard Snapshot

`GET /dashboard?range=7d&recentLimit=10`

Query params:
- `range`: optional, `today | 7d | 30d`, default `7d`
- `recentLimit`: optional, default `10`, max `20`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "range": "7d",
    "platform": {
      "apiVersion": "v1.2",
      "apiStatus": "RUNNING",
      "statusText": "Platform API 正常运行"
    },
    "agentCards": [
      {
        "taskType": "resume_evaluation",
        "taskTypeName": "简历评估",
        "description": "上传简历自动抽取信息、多维分析、深度洞察",
        "routePath": "/resume-evaluation",
        "taskTotal": 18,
        "successRate": 88.89,
        "lastRunTime": "2026-04-16 15:20:00"
      },
      {
        "taskType": "resume_interview",
        "taskTypeName": "模拟面试",
        "description": "根据候选人简历生成动态上下文闭环面试",
        "routePath": "/resume-interview",
        "taskTotal": 9,
        "successRate": 77.78,
        "lastRunTime": "2026-04-16 15:18:00"
      },
      {
        "taskType": "document_workspace",
        "taskTypeName": "文档知识",
        "description": "支持多文档聚合检索、问答与深度理解",
        "routePath": "/document-workspace",
        "taskTotal": 27,
        "successRate": 96.30,
        "lastRunTime": "2026-04-16 15:21:00"
      },
      {
        "taskType": "content_automation",
        "taskTypeName": "内容自动化",
        "description": "定制化流水线，从选题到发布全自动",
        "routePath": "/content-automation",
        "taskTotal": 12,
        "successRate": 83.33,
        "lastRunTime": "2026-04-16 15:16:00"
      }
    ],
    "overview": {
      "taskTotal": 66,
      "runningTotal": 2,
      "avgDurationMs": 21800,
      "modelCalls": 96,
      "successRate": 87.88,
      "totalTokens": 221882
    },
    "recentRuns": [
      {
        "traceId": "trace_001",
        "displayTaskId": "TSK-21",
        "taskId": "21",
        "taskType": "content_automation",
        "taskTypeName": "内容自动化",
        "taskSubType": "draft",
        "taskSubTypeName": "生成初稿",
        "durationMs": 128000,
        "totalTokens": 56600,
        "status": "SUCCESS",
        "statusText": "成功",
        "lastTime": "2026-04-16 15:21:08",
        "detailTraceId": "trace_001"
      }
    ]
  }
}
```

Frontend usage:
- homepage loads this API once
- `agentCards` renders the capability cards
- `overview` renders the KPI cards
- `recentRuns` renders the recent execution table
- click row action, then call:
  - `GET /api/v1/audit/execution/{traceId}`
  - `GET /api/v1/audit/execution/{traceId}/llm-calls`

## 2. Frontend Adjustments

1. Homepage only shows summary
- do not put trend charts and step funnels on homepage
- those stay inside `审计监控中心`

2. Menu adjustment
- keep `工作台大盘`
- keep each business module
- keep `审计监控中心`
- remove standalone first-level menu `运行详情分析`

3. KPI adjustment
- use:
  - total task count
  - running task count
  - average duration
  - model call count
  - success rate
  - total tokens
- do not use `知识库检索` and `Tool 调用` on homepage v1

4. Recent runs adjustment
- use `查看详情`, not `查看日志`
- detail page or drawer reuses existing audit detail APIs

5. Task label rendering
- primary label uses `taskTypeName`
- secondary label uses `taskSubTypeName` when not empty

## 3. Existing Detail APIs Reused

Execution detail:

`GET /api/v1/audit/execution/{traceId}`

LLM call detail:

`GET /api/v1/audit/execution/{traceId}/llm-calls`
