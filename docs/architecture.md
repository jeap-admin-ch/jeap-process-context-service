# Architecture

The Process Context Service (PCS) is a generic, largely **passive** service that provides a process context
to the microservices participating in a process in an event driven architecture.

## Goals

In an orchestration based architecture, the process context is maintained by a process engine. It essentially
comprises the process request, the state of the individual process steps and their results. Based on that
information the orchestrator decides which activities are required, when an activity can be started, and
parameterises the activities it starts.

An event driven architecture has no central orchestrator: services react to events on their own and execute
the required actions. That has advantages, but also drawbacks:

- **Transparency** — which process instances exist, what state are they in, what has happened so far?
- **Data flow** is harder, especially with indirect dependencies between worker services.
- **Complex dependencies** — as long as an activity is started by exactly one event this is simple, but as
  soon as several events have to be combined (join) it quickly becomes complex.

The PCS addresses these drawbacks by providing a central process context to the autonomously acting services.

### The PCS is strictly passive

It does:

- instantiate process instances when it receives a message that is configured to do so,
- listen to messages (events, commands) of the workers and update the process context,
- publish events about changes of the process context that workers can react to.

It is **not** an orchestrator and must never:

- decide which activities are to be executed — that is the responsibility of the workers,
- trigger an activity by sending a command.

## Domain terms

| Term                             | Meaning                                                                                                                     |
|----------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| Process                          | A set of logically connected activities (tasks) executed to reach a certain goal.                                            |
| Process template                 | Defines how a process runs: the expected tasks and their cardinality.                                                        |
| Process instance                 | An instance of a process, created based on a process template.                                                               |
| Task type                        | Description of a task within a process, in particular by task name and cardinality.                                          |
| Task instance                    | An instance of a task. Depending on the cardinality there can be several instances per task type.                            |
| Mandatory single-instance task   | Task with a cardinality of 1, has to be executed exactly once in the process.                                                |
| Optional single-instance task    | Task with a cardinality of 0..1.                                                                                            |
| Multi-instance task              | Task with a cardinality of 0..n.                                                                                            |
| Pending message                  | A message for which no process instance existed yet when it was received. It is assigned to the process instance as soon as that instance is created. |
| Process snapshot                 | The state of a process instance at a given point in time, for archiving by a Process Archive Service instance.               |

## Quality goals

| Prio | Category            | Quality goal    | Rationale                                                                                                                                    |
|------|---------------------|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| 1    | Functional suitability | Correctness  | Task and process states must be maintained consistently and the resulting events produced correctly — including under the asynchronous nature of event communication and eventual consistency. |
| 2    | Portability         | Adaptability    | Teams must be able to fulfil their process tracking requirements through configuration.                                                       |
| 3    | Maintainability     | Reusability     | Several teams use the same basis for the process context service of their business application.                                              |
| 4    | Usability           | Comprehensibility | Configuring process templates must be understandable and simple.                                                                            |
| 5    | Security            | Authenticity    | Access is protected by authentication and authorization (roles on UI and APIs, permissions on topics).                                        |
| 6    | Reliability         | Fault tolerance | Plugins, asynchronous communication with eventual consistency and temporary inconsistencies between instances and templates during deployments are possible sources of error; the service is designed to cope with smaller inconsistencies and to handle the rest robustly. |

High **availability** is explicitly less important: because of the decoupling and the passive character of
the service, and because the frontend serves traceability and analysis rather than business-critical
operations.

## Context

### Business context

| Data flow                       | Description                                                                                                                                                   |
|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Messages (domain events, commands) | Messages relevant for the process state. They contain a process ID or can be correlated to a process instance via a reference or their payload. They can create new process instances if the template says so. |
| `ProcessSnapshotCreatedEvent`   | Event notification when a process snapshot has been created.                                                                                                   |
| Process state                   | The state of the process and its tasks, presented as a checklist in a user interface, and provided as a process snapshot for archiving.                        |

### Technical context

| Component               | Channel        | Description                                                                                                                                            |
|-------------------------|----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| Browser                 | HTTPS          | Read-only access to the REST API to display the state of a process.                                                                                     |
| Microservices           | Kafka          | Produce domain events that the PCS consumes to track the state of the process instances.                                                                 |
| Process Archive Service | Kafka, HTTPS   | Is informed about new snapshots by the `ProcessSnapshotCreatedEvent` and fetches the snapshots for archiving via the REST API.                           |
| Error Handling Service  | Kafka          | `MessageProcessingFailedEvent`s for messages whose processing failed are produced to the error topic; retries are published back to the original topic. |

## Building blocks

| Component                       | Description                                                                                             |
|---------------------------------|-----------------------------------------------------------------------------------------------------------|
| Domain services                 | Domain logic and access across several aggregate roots, transaction control.                             |
| Domain model                    | Domain logic within one aggregate root (e.g. state changes while processing messages).                   |
| Message consumer port           | Receives messages that can lead to changes of the process state.                                         |
| Query port                      | Provides query capabilities for processes (implemented as a read-only repository).                       |
| Event producer port             | Produces the process status notification events.                                                         |
| ProcessInstance repository (port) | Repository interface for process instances.                                                            |
| ProcessSnapshot repository (port) | Repository interface for persisting process snapshots.                                                 |

### REST API

| Path                                                | Verb | Response                   | Authorization                |
|-----------------------------------------------------|------|----------------------------|------------------------------|
| `/api/processes/`                                   | GET  | Process instances (paged)  | `processinstance` / `view`   |
| `/api/processes/{originProcessId}`                  | GET  | Process instance           | `processinstance` / `view`   |
| `/api/processes/{originProcessId}/messages`         | GET  | Messages (paged)           | `processinstance` / `view`   |
| `/api/processes/{originProcessId}/process-data`     | GET  | Process data (paged)       | `processinstance` / `view`   |
| `/api/processes/{originProcessId}/process-relations`| GET  | Process relations (paged)  | `processinstance` / `view`   |
| `/api/processes/{originProcessId}/relations`        | GET  | Relations (paged)          | `processinstance` / `view`   |
| `/api/snapshot/{originProcessId}`                   | GET  | A process snapshot version | `processsnapshot` / `view`   |
| `/api/reevaluation-jobs/{jobId}`                    | PUT  | Empty `200` response       | `processcontextjob` / `write` |
| `/api/reevaluation-jobs/{jobId}`                    | GET  | YAML job report            | `processcontextjob` / `read` |

The process view role is typically granted to the business user of the frontend via the OAuth authorization
code flow. An up-to-date description of the API is served by the running application at
`/swagger-ui/index.html`.

In addition, `/api/configuration`, `/api/configuration/version` and `/api/configuration/log-deeplink` serve
the bundled Angular UI with its OIDC configuration, the application version and the
[log deep link template](configuration.md#log-deep-link).

### Aggregate roots

| Aggregate         | Description                                                                                                     |
|-------------------|-------------------------------------------------------------------------------------------------------------------|
| Process instance  | Process instance with its state, task instances and process template.                                            |
| Process template  | The template defining the tasks to be executed in a process.                                                     |
| Pending message   | Messages that could not yet be correlated to an existing process instance when they were received.               |
| Message           | Messages (domain events, commands) with their message key/value data.                                            |
| Process snapshot  | The representation of a process instance at a given point in time.                                               |
| Maintenance job   | Durable relation-reevaluation request containing one independently tracked task per process.                    |

A process snapshot deliberately does not represent the complete state of a process instance, only the part
needed for traceability. Snapshots are stored in the Avro format recommended for archiving by the Process
Archive Service.

## Messages

### Published events

| Event type                    | References        | Payload           | Description                            |
|-------------------------------|-------------------|-------------------|----------------------------------------|
| `ProcessSnapshotCreatedEvent` | `originProcessId` | `snapshotVersion` | A new process snapshot version was created. |

### Consumed messages

Domain events and commands, depending on the business application. References to process and task are
extracted by correlation providers, data by custom payload and reference extractors.

### Internal messages

Internal Kafka messages decouple the processing steps inside the PCS. They decouple the processing from the
partitioning of the source topics, shorten transactions, simplify error handling and generally increase
robustness. These technical messages are modelled as jEAP domain events.

| Topic (configuration key)  | Message type                  | Payload                                                                               | Description                                                                                     |
|----------------------------|-------------------------------|---------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| `jeap.processcontext.kafka.topic.process-outdated-internal` | `ProcessContextOutdatedEvent` | `originProcessId`, `processUpdateType`, optional message/template data or maintenance task envelope | Something happened for a process instance that potentially affects its state, or a durable relation-reevaluation task must be executed. |

Maintenance events are written through the transactional outbox with the origin process ID as Kafka key. An instance
with maintenance enabled must declare both producer and consumer contracts for `ProcessContextOutdatedEvent` on the
configured internal topic.

## Cross-cutting concepts

### Architectural pattern

**Ports and adapters**, with an active domain model implementing the domain logic:

- the domain model is at the centre and can be tested separately,
- it is not anaemic — no models consisting of pure getters and setters,
- computation of state transitions is encapsulated centrally in the model instead of being distributed,
- plugins of the business applications are implemented as adapters against ports.

### Persistence

- **Process instance**: the domain model is persisted directly with JPA annotations on the fields of the
  class. This avoids unnecessary mapping between JPA and the domain model; an independent persistence model
  would be desirable but hard to justify given the mapping effort.
- **Process template**: loaded as JSON, persisted in the database.
- **Process snapshot**: stored in an S3 object store as an Avro binary, together with the metadata required
  by the Process Archive Service.

### Asynchronous notifications and eventual consistency

Message processing per process instance must **not** happen in parallel. Serial processing per process
instance is essential — the *order* does not matter, only the avoidance of race conditions. This is achieved
with the internal processing events described above: they use the process instance ID as the Kafka record
key, which serialises the consumption of the events per process instance ID.

### Late correlation

Correlation based on process data allows a message to be assigned to a process instance based on information
that becomes known to the instance only after the process has started.

- If the message **writing** the process data is processed **before** the message to be correlated, the
  correlation already happens in the `ProcessUpdateService` by searching for a process with the relevant
  process data (*early correlation*).
- If the messages arrive in the opposite order, that search finds nothing. The message is stored first and
  correlated later, when the relevant process data becomes known (*late correlation*). Late correlation
  happens in the `ProcessInstanceService`, which writes the process data: whenever it creates new process
  data, it checks whether already received messages can now be correlated.

Correlation must always happen, even if an early and a late correlation attempt occur practically at the same
time. To prevent race conditions, processing in both services is split into two transactions: first the data
derived from the message is stored, then the database queries detecting correlations are executed. This
guarantees a correlation regardless of how the transactions of the two services interleave.

### Idempotency

- **Process creation** is idempotent via the origin process ID: if an instance already exists, the creation
  is ignored.
- Events triggered by state changes are published successfully **before** the triggering message is
  acknowledged. If processing fails, the original message is passed to the error handling and acknowledged;
  a retry then relies on the idempotent processing again.

### Error handling

The Error Handling Service is used for errors occurring while processing a message. Errors are classified as:

| Exception                                              | Classification |
|--------------------------------------------------------|----------------|
| `org.springframework.dao.TransientDataAccessException` | Temporary      |
| `org.apache.kafka.common.errors.RetryableException`    | Temporary      |
| Everything else                                        | Permanent      |

### Logging

| Level | Usage                                | Examples                                                                        |
|-------|--------------------------------------|---------------------------------------------------------------------------------|
| ERROR | Errors                               |                                                                                 |
| WARN  | Unexpected states that are not necessarily errors |                                                                    |
| INFO  | State transitions in processes       | Startup information, process instance creation, process state changes, planning and completion of tasks |
| DEBUG | Further details for analysis         | Request/response details, consumed and produced records                          |

## Design decisions

### Architectural pattern

**Decision**: ports and adapters with an active domain model.

**Rationale**: clear structure, transaction boundaries and a clear location for the domain logic; separation
of infrastructure and domain; plugins of the business applications can be implemented against ports.
The alternative considered was a layered architecture, which gives a less clearly defined structure.

### Persistence

**Decision**: the domain model is persisted directly with JPA.

**Rationale**: lower effort and complexity. A move to event sourcing remains possible if it later turns out
to be necessary.

| Option | Description                                                     | Advantages                                                            | Disadvantages                                                                                                     |
|--------|-----------------------------------------------------------------|-----------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| 1 (chosen) | Domain model persisted directly with JPA                    | Low effort, simple implementation                                     | Domain model has JPA dependencies; only the current state is known, not the history                                |
| 2      | Domain model in memory only, persistence via event sourcing     | Good traceability, good expected performance through inserts only     | Database harder to analyse, higher implementation effort, more complex through the decoupling of model and persistence |

### Asynchronous processing of messages affecting the process state

**Decision**: an internally event driven architecture. The algorithm has three stages:

1. consume the incoming message, persist the message type in the process context and produce a technical
   event to update the process and task state,
2. compute the updates of the process and task state and produce a technical event to trigger the reactions
   to the new state,
3. compute the reactions (snapshots, completion), persist them and produce the corresponding domain events.

The integration between the three stages happens over technical Kafka events. This keeps the transaction
boundaries small, and the partitioning — and with it the parallelism — independent of the source topics.

**Rationale**: clean decoupling, no database-centric design that would not scale.

Alternatives considered were pessimistic locking on a dedicated lock table (scales badly, optimises for the
special case of parallel processing), forwarding the incoming events to an internal topic keyed per process
instance (requires a message contract for every forwarding, exposing teams to PCS internals), and optimistic
locking (acceptable, but may produce state events more than once).
