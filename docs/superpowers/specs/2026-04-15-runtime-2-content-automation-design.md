# Runtime 2.0 And Content Automation Design

## Goal

Upgrade the current execution layer from a single fixed four-step chain into a runtime that supports multiple execution modes, while adding the first version of content automation with constrained draft publishing.

## Scope

This design covers two linked changes:

1. Runtime 2.0 with three execution modes:
   - `SINGLE_SHOT`
   - `STRUCTURED_PLAN_EXECUTE`
   - `OPEN_PLAN_EXECUTE`
2. Content automation v1 running on `STRUCTURED_PLAN_EXECUTE` with:
   - topic planning
   - outline generation
   - draft generation
   - polish
   - compliance review
   - publish planning
   - mock publish execution
   - publish summary

## Constraints

- Evolve the current project. Do not rewrite the platform.
- Keep the existing dynamic client/model/MCP/advisor assembly.
- Keep resume evaluation and mock interview behavior compatible.
- Document workspace remains `SINGLE_SHOT`.
- Mock publish only. No real platform publishing in this round.

## Runtime 2.0

### Execution modes

- `SINGLE_SHOT`
  - one retrieval/generation cycle
  - no multi-stage workflow loop
  - used by document workspace tasks

- `STRUCTURED_PLAN_EXECUTE`
  - fixed, explicit stage chain
  - stable intermediate outputs
  - used by resume evaluation, mock interview, content automation

- `OPEN_PLAN_EXECUTE`
  - reserved runtime branch
  - no production business flow in this round
  - only skeleton and extension point

### Runtime structure

Introduce a dispatcher layer:

```text
AgentRuntimeDispatcher
  -> SingleShotExecutionHandler
  -> StructuredPlanExecuteHandler
  -> OpenPlanExecuteHandler
```

Introduce common runtime request fields on execution commands:

- `taskType`
- `subType`
- `executionMode`

Default existing auto-agent requests to:

- `taskType = legacy_auto_agent`
- `executionMode = STRUCTURED_PLAN_EXECUTE`

### Structured workflows

`STRUCTURED_PLAN_EXECUTE` will no longer mean only the old four steps. It becomes a workflow family:

```text
STRUCTURED_PLAN_EXECUTE
  -> LegacyAutoAgentWorkflow
  -> ContentAutomationWorkflow
```

The existing Step1/2/3/4 chain remains as the legacy workflow.

## Content Automation V1

### Positioning

This is a structured workflow, not a generic chat flow and not a fully open agent.

### Workflow

```text
TopicPlannerNode
-> OutlineGeneratorNode
-> DraftGeneratorNode
-> PolishVerifierNode
-> ComplianceReviewerNode
-> PublishPlannerNode
-> PublishExecutorNode
-> PublishSummarizerNode
```

### Publish boundary

Only draft publishing is supported in this round.

Publish execution is abstracted through a mock publisher adapter:

- input: `PublishCommand`
- output: `PublishResult`

No real platform credentials or third-party API integration are required in this round.

## Data model

Add MySQL tables:

- `content_task`
- `content_task_step`

`content_task` stores task-level state.
`content_task_step` stores each stage output for UI replay and auditing.

## API surface

Add content automation APIs:

- `POST /api/v1/content/task/create`
- `POST /api/v1/content/task/execute/stream`
- `GET /api/v1/content/task/{taskId}`
- `GET /api/v1/content/task/{taskId}/steps`

## Error handling

Reuse the runtime resilience layer:

- timeout
- retry
- degrade
- unified error code

For content automation:

- generation stages may degrade to shorter fallback outputs
- publish planning failure blocks publish execution
- publish execution failure still writes a final summary result

## Testing

Minimum required verification:

1. Runtime dispatcher routes by execution mode and task type
2. Legacy workflow still runs through structured mode
3. Content task create/query works
4. Content workflow executes all stages
5. Mock publish returns stable draft result
6. SSE emits structured step events
