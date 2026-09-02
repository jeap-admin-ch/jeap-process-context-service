# Maintenance jobs

PCS maintenance jobs repair or repeat derived process-context state without changing the process template or replaying
the original business messages. They support three controlled operator operations:

- **Relation reevaluation** recalculates relations for existing process instances using the deployed template.
- **Process-data backfill** adds missing process data and then performs a complete relation reevaluation.
- **Relation republication** passes selected persisted relations to the configured `RelationListener` again.

Maintenance is not a bulk data editing API. Requests identify every process or relation explicitly, all work is tracked
durably, and the configured limits protect an instance from unsafe workloads.

For command syntax, CSV handling, and client-side validation, use the
[jEAP CLI PCS maintenance documentation](https://github.com/jeap-admin-ch/jeap-cli/blob/main/docs/pcs-maintenance.md).
This page documents the PCS server contract and operational behavior instead of duplicating the CLI guide.

## Enablement and dependencies

Maintenance APIs are disabled by default. Enable them only for controlled operator access:

```yaml
jeap:
  processcontext:
    maintenance:
      enabled: true
    kafka:
      topic:
        process-outdated-internal: jme-process-event-received
        add-process-data-command: jme-add-process-data-command
```

The full limits configuration is described in [Configuration](configuration.md#maintenance-jobs).

A PCS instance using `jeap-process-context-scs`, or inheriting from `jeap-process-context-service-instance`, already
contains the REST, JPA, Kafka, and transactional-outbox dependencies required by maintenance jobs. The instance needs:

- PostgreSQL migrations from the matching PCS version;
- the default Kafka cluster and both configured internal topics;
- the transactional outbox relay enabled and operating;
- a `RelationListener` when relation notifications or republication are required.

`ProcessContextOutdatedEvent` and `AddProcessDataCommand` are internal PCS messages. They are supplied by PCS and exempt
from application message-contract validation, so an application does not declare producer or consumer contracts for
them. Existing business-message and relation-listener contracts remain unchanged.

## Security

The endpoints use OAuth2 bearer tokens and semantic roles:

| Operation | Required role |
| --- | --- |
| Submit a job | `processcontextjob:write` |
| Retrieve a report | `processcontextjob:read` |
| Retrieve relation UUIDs | `processinstance:view` |

For a system named `jme`, OAuth role names are normally `jme_@processcontextjob_#write` and
`jme_@processcontextjob_#read`. Retrieving relation UUIDs additionally requires `jme_@processinstance_#view`, which
grants read access to process instance views and their associated data.

## REST and YAML contract

Requests and reports use YAML. PCS accepts `application/yaml` and the legacy `application/x-yaml` media type.

| Operation | Submit | Report |
| --- | --- | --- |
| Relation reevaluation | `PUT /api/reevaluation-jobs/{jobId}` | `GET /api/reevaluation-jobs/{jobId}` |
| Process-data backfill | `PUT /api/backfill-jobs/{jobId}` | `GET /api/backfill-jobs/{jobId}` |
| Relation republication | `PUT /api/relation-publication-jobs/{jobId}` | `GET /api/relation-publication-jobs/{jobId}` |

A new job returns `201 Created`. Reusing a job UUID with equivalent normalized content is idempotent and returns
`200 OK`. Reusing it with different content returns `409 Conflict`. Invalid YAML or request content returns
`400 Bad Request`; authorization failures return `403 Forbidden`; an unknown report UUID returns `404 Not Found`.

The canonical ready-to-use request, CSV, and report files are under
[`docs/examples/maintenance`](examples/maintenance/reevaluation-request.yaml). The request and report YAML files are the
golden examples also used by PCS OpenAPI and jEAP CLI tests.

## Common task lifecycle

Every target is an independently tracked durable task. Reevaluation and republication tasks start in
`EVENT_QUEUED`; backfill tasks start in `COMMAND_QUEUED`:

```text
COMMAND_QUEUED -> EVENT_QUEUED -> SUCCEEDED | NOT_FOUND | FAILED
                  EVENT_QUEUED -> SUCCEEDED | NOT_FOUND | FAILED
```

`COMMAND_QUEUED`, `EVENT_QUEUED`, and the terminal states are persisted report states. Processing occurs while a
consumer owns the locked task and is intentionally not persisted as a separate state. Until the terminal transaction
commits, a concurrent report still shows the preceding queued state.

Every task is ultimately processed through a keyed `ProcessContextOutdatedEvent`:

- reevaluation uses update type `REEVALUATE_JOB`;
- backfill uses update type `BACKFILL_JOB`;
- republication uses update type `REPUBLISH_RELATION_JOB`.

Backfill first publishes `AddProcessDataCommand`. Its consumer locks the durable task, inserts the process data, and
atomically bridges that same task to `ProcessContextOutdatedEvent(BACKFILL_JOB)`. The command directly persists the new
process data; the backfill event then owns the complete process reevaluation and relation-notification handoff.

Jobs remain `open` while any task is non-terminal and become `completed` when all tasks are terminal. The result is
`succeeded`, `partially-succeeded`, or `failed`. Reports contain job metadata and each target's task ID, state, and
sanitized error details. Backfill reports deliberately omit the submitted process-data values. Scalar identifiers that
resemble numbers remain quoted strings in YAML.

## Delivery and failure semantics

Job, task, and outgoing message writes use the transactional outbox. Transport recovery is at least once: an outbox or
Kafka message can be delivered more than once, and consumers use the durable task state to make duplicate processing
safe where possible.

A caught processing exception is persisted as terminal `FAILED` and the Kafka message is acknowledged. It is not sent
to jEAP Error Handling and does not receive an automatic application retry. Another operator attempt requires a new job
UUID. If the terminal result itself cannot be persisted, the message is not acknowledged and transport recovery retries
it.

`SUCCEEDED` describes processing controlled by PCS:

- reevaluation means relation calculation and configured listener handoff completed;
- backfill means data insertion, complete reevaluation, and configured listener handoff completed;
- republication means the configured `RelationListener` returned successfully.

None of these results proves that a downstream system consumed a notification. Downstream consumers remain responsible
for their own delivery confirmation and deduplication.

## Relation reevaluation

Reevaluation recalculates relations for explicitly selected process instances against the currently deployed relation
patterns. It does not change process data.

```yaml
process-template-name: assessmentProcess
processes:
  - origin-process-id: assessment-4711
  - origin-process-id: assessment-4712
```

Canonical files:

- [request YAML](examples/maintenance/reevaluation-request.yaml)
- [process CSV](examples/maintenance/reevaluation-processes.csv)
- [report YAML](examples/maintenance/reevaluation-report.yaml)

Missing processes become `NOT_FOUND`. Candidate relations are selected in pages and bounded by the configured maximum
candidate count. Relation-listener failures become `FAILED`.

## Process-data backfill

Backfill adds one or more process-data values to each selected process and performs a complete reevaluation. Every entry
requires a non-blank origin process ID and at least one value. Every value requires `key` and `value`; `role` is optional.

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

Canonical files:

- [request YAML](examples/maintenance/backfill-request.yaml)
- [process-data CSV](examples/maintenance/backfill-process-data.csv)
- [report YAML](examples/maintenance/backfill-report.yaml)

Duplicate commands do not insert the values or queue the event twice. A missing process becomes `NOT_FOUND`; validation,
data insertion, reevaluation, or listener failures become `FAILED`.

## Relation republication

Republication passes explicitly selected persisted relations to the configured `RelationListener` without creating or
changing them. The request accepts persisted relation row UUIDs only. Retrieve these UUIDs from the `id` field returned
by `GET /api/processes/{originProcessId}/relations`. PCS deliberately provides no all-relations or maintenance filtering
mode; relation selection is an operator responsibility.

```yaml
relationIds:
  - 019c8c72-6fd1-7f25-a9a1-3b3d51fbb321
  - 019c8c72-7b42-7a04-9443-bf8ec98ce871
```

Canonical files:

- [request YAML](examples/maintenance/relation-publication-request.yaml)
- [relation CSV](examples/maintenance/relation-publication-relations.csv)
- [report YAML](examples/maintenance/relation-publication-report.yaml)

The consumer reloads the persisted relation and reuses its stable idempotence ID. A relation missing at execution time
becomes `NOT_FOUND`; a disabled relation feature flag or listener exception becomes `FAILED`. At-least-once recovery can
invoke the listener more than once with the same idempotence ID.

## Operational limitations

- Respect the configured task, request-size, field-length, process-data, and relation-candidate limits.
- Use separate smaller jobs instead of raising limits for an unbounded request.
- Do not use maintenance jobs as a replacement for normal event processing or data migration.
- Select and review relation UUIDs from the process relations API before republication; PCS does not discover targets
  for an operator.
- Monitor reports until the job is `completed`; HTTP acceptance only confirms durable submission.
- Keep the outbox relay and both internal Kafka topics available until all tasks are terminal.
