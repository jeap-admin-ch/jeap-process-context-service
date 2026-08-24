# Maintenance jobs

Maintenance job APIs are disabled by default and must be enabled as described in
[Configuration](configuration.md#maintenance-jobs). They use OAuth2 bearer-token authentication. Submitting a job
requires `processcontextjob:write`; retrieving a report requires `processcontextjob:read`.

Requests and reports use YAML. Both `application/yaml` and the legacy `application/x-yaml` media type are supported.
A new job returns `201 Created`. Reusing a job ID with equivalent normalized content is idempotent and returns `200 OK`;
reusing it with different content returns `409 Conflict`. Invalid YAML or request content returns `400 Bad Request`.

Reports contain job metadata and the state, origin process ID, and possible error of every task. Process-data backfill
reports deliberately do not expose the durable values submitted with the job. Scalar values that resemble numbers are
quoted so identifiers retain their YAML string type.

## Relation reevaluation

`PUT /api/reevaluation-jobs/{jobId}` submits processes whose relations must be reevaluated against the currently
deployed relation patterns. `GET /api/reevaluation-jobs/{jobId}` returns the report. Canonical request fields are
`process-template-name` and `origin-process-id`; the legacy aliases `processTemplateName` and `originProcessId` remain
accepted.

Jobs, `event-queued` tasks, and keyed `ProcessContextOutdatedEvent` messages are written atomically through the
transactional outbox. Missing processes and processing failures are recorded as terminal task results and acknowledged;
an event is retried only when its terminal result cannot be persisted. The outbox relay must remain enabled. Events use
the configured `jeap.processcontext.kafka.topic.process-outdated-internal` topic.

## Process-data backfill

`PUT /api/backfill-jobs/{jobId}` adds process data to existing process instances and reevaluates their relations.
Every entry requires a non-blank origin process ID and at least one process-data value. Every value requires non-blank
`key` and `value` fields; `role` is optional.

```yaml
process-template-name: assessmentProcess
entries:
  - origin-process-id: assessment-4711
    process-data:
      - key: assessmentArtefactId
        value: art-456
        role: FinalVersion
      - key: assessmentId
        value: a-123
  - origin-process-id: assessment-4712
    process-data:
      - key: assessmentId
        value: a-789
```

The canonical fields are `process-template-name`, `origin-process-id`, and `process-data`. The legacy camel-case aliases
`processTemplateName`, `originProcessId`, and `processData` remain accepted.

Submitting the job writes its durable `command-queued` tasks and keyed `AddProcessDataCommand` messages in one
transaction. Each command contains the complete normalized durable values. Its consumer reloads and locks the durable
task, validates the command against it, adds the process data, and writes a keyed `ProcessContextOutdatedEvent` with
update type `BACKFILL_JOB` and the `event-queued` state in one transaction. Duplicate commands are acknowledged without
adding process data or writing another event. The event reevaluates relations and completes the task. Processing failures
are acknowledged after the failed task result has been persisted; if that persistence fails, the command or event is
retried.

The outbox relay must remain enabled. Both maintenance message types are internal to PCS and do not require
application producer or consumer contracts.

`GET /api/backfill-jobs/{jobId}` returns a YAML report:

```yaml
job-id: 88dbb65f-9634-4685-bc86-17b72d715d3e
job-type: process-data-backfill
process-template-name: assessmentProcess
job-state: open
started: 2026-08-06T08:03:12Z
started-by-name: John Doe
started-by-ext-id: "287365"
entries:
  - task-id: 019c8c72-6fd1-7f25-a9a1-3b3d51fbb321
    origin-process-id: assessment-4711
    state: command-queued
```

A job starts in `open` state. Backfill tasks move from `command-queued` to `event-queued`, where they remain until
atomically reaching `succeeded`, `not-found`, or `failed`. Tasks for different processes in the same job can run
concurrently; the job result is finalized under a short shared-job lock after task processing. The job becomes
`completed` when all tasks are terminal and records a `succeeded`, `partially-succeeded`, or `failed` result. A failed
task can include an error message and trace ID.
