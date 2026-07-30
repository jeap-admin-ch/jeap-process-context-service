# Process Templates

A process definition is described as a JSON document, the **process template**. The PCS loads its templates
from the classpath matching the pattern `classpath:/process/templates/*.json`, configurable with
`jeap.processcontext.template.classpath-location-pattern`.

A JSON schema supporting code completion and validation in the IDE is available at
`jeap-process-context-repository-template-json/src/main/schema/process-template-schema.json`, see
[Getting Started](getting-started.md#json-schema-support-in-the-ide).

## Structure

```json
{
  "name": "raceProcess",
  "tasks": [ ... ],
  "messages": [ ... ],
  "processData": [ ... ],
  "processRelationPatterns": [ ... ],
  "relationSystemId": "ch.admin.jme.Race",
  "relationPatterns": [ ... ],
  "completions": [ ... ],
  "snapshots": [ ... ]
}
```

The template starts with the name of the process template:

```json
{
  "name": "testProcess"
}
```

## Tasks

A task represents a (normally automated) step of work in a process. The template defines which task types a
process contains. The process is completed when all its task types have been planned and completed.
Dependencies between task types are currently not modelled.

At runtime, task instances are created in a process based on the template. A task type follows a certain
**lifecycle** and a certain **cardinality**:

| Lifecycle  | Task instantiation             | Default cardinality | Initial state | Further possible states                         |
|------------|--------------------------------|---------------------|---------------|-------------------------------------------------|
| `static`   | When the process is created    | `single-instance`   | `PLANNED`     | `COMPLETED`                                     |
| `dynamic`  | By messages                    | `multi-instance`    | `NOT_PLANNED` | `PLANNED` (1..n instances), `NOT_REQUIRED` (0 instances), `COMPLETED` |
| `observed` | After receiving a message      | `multi-instance`    | `NOT_PLANNED` | `COMPLETED`                                     |

| Cardinality       | Task instantiation                                          |
|-------------------|-------------------------------------------------------------|
| `single-instance` | Only one task instance of the task type can be created.     |
| `multi-instance`  | Several task instances of the task type can be created.     |

For a `dynamic` task the task instance ID is provided by a correlation provider. For an `observed` task the
task ID is irrelevant, as the task can never be modified after its instantiation.

The first task below is a mandatory static single-instance task — `lifecycle=static` and
`cardinality=single-instance` are the defaults — completed when a certain message is received. The second one
is a dynamic multi-instance task whose instance count is planned by a message:

```json
"tasks": [
  {
    "name": "singleTask",
    "completedBy": {
      "message": "MyExampleEvent"
    }
  },
  {
    "name": "dynamicTaskCompletedByDomainEvent",
    "lifecycle": "dynamic",
    "plannedBy": {
      "message": "MyPlanningEvent"
    },
    "completedBy": {
      "message": "MyExampleEvent"
    }
  }
]
```

> Task templates carry no display text: the label shown in the UI comes from the translations, keyed by the
> task name (see [Internationalization](#internationalization)). The template model rejects unknown fields, so
> a stray attribute makes the PCS fail to load the template at startup.

> **Legacy cardinality configuration.** PCS versions before 5.13.0 only knew `cardinality` with the values
> `single` and `dynamic`. Newer versions still support them and translate them as
> `cardinality=single` → `lifecycle=static, cardinality=single-instance` and
> `cardinality=dynamic` → `lifecycle=dynamic, cardinality=multi-instance`. Migrating the old configuration
> is recommended, as it will no longer be supported in the future: replace `cardinality=single` with
> `lifecycle=static` and `cardinality=dynamic` with `lifecycle=dynamic`.

### Tasks planned by messages

Tasks can be planned by messages. Such tasks are always planned dynamically and are optional. Optional tasks
are currently only supported with the cardinality `multi-instance` (0..n).

```json
"lifecycle": "dynamic",
"plannedBy": {
  "message": "JmeCarScannedEvent"
}
```

These tasks are only instantiated when the specified message arrives — in contrast to conventional tasks, no
instance in the state `NOT_PLANNED` is held. An optional task therefore only blocks the process state when it
has been created and not completed; an unplanned optional task has no influence on the process state.

### Task instantiation conditions

For dynamic tasks, a **task instantiation condition** can compute programmatically from the incoming message
whether the task should be instantiated. The task is only created if the condition returns `true`. The
condition is configured in the `plannedBy` block:

```json
{
  "name": "MyConditionalTask",
  "lifecycle": "dynamic",
  "plannedBy": {
    "message": "TestEvent",
    "condition": "ch.admin.bit.jeap.processcontext.repository.template.json.TestTaskInstantiationCondition"
  }
}
```

Conditions implement `TaskInstantiationCondition`:

```java
public interface TaskInstantiationCondition {

    /**
     * @return true if the task should be instantiated, false otherwise
     */
    boolean instantiate(Message message);
}
```

An example implementation:

```java
public class SimpleInstantiationCondition implements TaskInstantiationCondition {
    @Override
    public boolean instantiate(Message message) {
        return message.getMessageData().stream().anyMatch(messageData ->
                messageData.getKey().equals("someField") && messageData.getValue().equals("foo"));
    }
}
```

### Task completion by a message

Tasks reach the state `COMPLETED` by a message. The state change of tasks is evaluated whenever the process
context changes:

- **single-instance tasks**: completed when a message of the referenced type correlated to the process
  instance is received,
- **multi-instance tasks**: completed when a message of the referenced type correlated to the process
  instance is received whose task ID matches the task.

```json
"completedBy": {
  "message": "JmeCarScannedEvent"
}
```

### Observation tasks

An observation task is in principle the projection of a message into the task list. Its purpose is to
visualise important occurrences for the business user within the task list:

```json
{
  "name": "objectsOnRoadSpotted",
  "lifecycle": "observed",
  "observes": {
    "message": "JmeRaceObjectsOnRoadSpottedEvent"
  }
}
```

When the message arrives, the observation task is instantiated and immediately assumes the state `COMPLETED`.
Specifying `"lifecycle": "observed"` is optional, since `observes` already determines the lifecycle.

The instantiation of an observation task can be controlled by a condition as well. The interface is the same
(`TaskInstantiationCondition`), but the condition is configured in the `observes` block:

```json
{
  "name": "triggerSafetyCar",
  "observes": {
    "message": "JmeRaceObjectsOnRoadSpottedEvent",
    "condition": "ch.admin.bit.jeap.jme.processcontext.condition.TriggerSafetyCarTaskInstantiationCondition"
  }
}
```

### Task data

A task can be linked with message data from the messages that started or completed it. The UI then displays
this data on the task, which allows arbitrary additional information about the start or the end of a task to
be documented.

Declaring task data requires a message type from which message data is to be extracted, plus the keys of the
message data entries to extract:

```json
{
  "name": "raceCarRefuel",
  "plannedBy": {
    "message": "JmeRaceDestinationReachedEvent"
  },
  "completedBy": {
    "message": "JmeRaceCarRefuellingCompletedEvent"
  },
  "taskData": [
    {
      "sourceMessage": "JmeRaceDestinationReachedEvent",
      "messageDataKeys": ["parkingSpotNumber"]
    },
    {
      "sourceMessage": "JmeRaceCarRefuellingCompletedEvent",
      "messageDataKeys": ["fuelType", "fuelAmount"]
    }
  ]
}
```

A declared `sourceMessage` must reference a message type that starts or completes the task, i.e. a type
already listed in `plannedBy` or `completedBy`. The declared `messageDataKeys` must address message data
entries provided by a payload or reference extractor for that message type.

Translations can be defined for the extracted keys, see [Internationalization](#internationalization).

### User data

A task is automatically linked by the PCS with the user data (`MessageUser`) of the messages that started or
completed it. The UI displays this data on the tasks under *started by* and *completed by*. All available
user data is shown; translations can be defined for the identifiers.

## Messages

The `messages` block references the messages to be consumed.

| Attribute                      | Required  | Description                                                                                                                                                                    |
|--------------------------------|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `messageName`                  | mandatory | Name of the message type as defined in the message type registry.                                                                                                               |
| `topicName`                    | mandatory | The Kafka topic to consume the message from.                                                                                                                                    |
| `clusterName`                  | optional  | The logical Kafka cluster name where the topic exists. Default: the default cluster.                                                                                            |
| `correlationProvider`          | optional  | A `MessageCorrelationProvider` extracting process IDs and optionally task IDs from a message. Default: messages are correlated via the message attribute `processId`, no task ID is extracted. |
| `payloadExtractor`             | optional  | A `PayloadExtractor` extracting data from the payload of a message.                                                                                                             |
| `referenceExtractor`           | optional  | A `ReferenceExtractor` extracting data from the references of a message.                                                                                                        |
| `correlatedBy`                 | optional  | Declares that a message is correlated to a process instance because one of its message data entries matches a process data entry, see [Correlation by process data](#correlation-by-process-id-vs-by-process-data). |
| `triggersProcessInstantiation` | optional  | If `true`, receiving this message creates a process instance unless one with that process ID already exists. If `false`, no process instance is created, even if a `processInstantiationCondition` is configured. Default: `null`. |
| `processInstantiationCondition`| optional  | A `ProcessInstantiationCondition` deciding, based on the message, whether the process is instantiated. Not evaluated if `triggersProcessInstantiation` is `false`.               |

Data extracted by a payload or reference extractor is persisted as `MessageData` and is available for further
processing, e.g. in a custom condition.

```json
"messages": [
  {
    "messageName": "JmeCarEnteredEvent",
    "topicName": "jme-process-car-entered"
  },
  {
    "messageName": "JmeCarScannedEvent",
    "topicName": "jme-process-car-scanned",
    "clusterName": "my-cluster",
    "correlationProvider": "ch.admin.bit.jeap.jme.processcontext.event.JmeCarScannedEventCorrelationProvider",
    "payloadExtractor": "ch.admin.bit.jeap.jme.processcontext.event.JmeCarScannedEventPayloadExtractor",
    "referenceExtractor": "ch.admin.bit.jeap.jme.processcontext.event.JmeCarScannedEventReferenceExtractor",
    "correlatedBy": {
      "processDataKey": "some-process-data-key",
      "messageDataKey": "some-message-data-key"
    },
    "triggersProcessInstantiation": true
  }
]
```

### Process instantiation

To create a process instance when a message is received, set `triggersProcessInstantiation` or
`processInstantiationCondition` on the message declaration. In the first case the instance is always created,
in the second only if the configured condition is fulfilled:

```json
"messages": [
  {
    "messageName": "JmeRacePreparedEvent",
    "topicName": "jme-race-prepared",
    "processInstantiationCondition": "ch.admin.bit.jeap.jme.processcontext.condition.RacePreparedProcessInstantiationCondition"
  }
]
```

```java
public class RacePreparedProcessInstantiationCondition implements ProcessInstantiationCondition<JmeRacePreparedEvent> {
    @Override
    public boolean triggersProcessInstantiation(JmeRacePreparedEvent event) {
        // Don't create a process if the race car id is "test-car"
        return !"test-car".equals(event.getPayload().getRaceCarNumber());
    }
}
```

The PCS only creates a new process instance if no instance with the given process ID exists yet. The process
ID — a natural business ID or a UUID — is read from the message attribute `processId` by default, or provided
by a correlation provider.

`triggersProcessInstantiation` can be specified in addition to a condition: setting it to `false` switches
the condition off, so no instance is created even if the condition is fulfilled.

> **A message may only start a process for one process template.** The same message type may configure the
> start of a process in several templates, but then the message declarations must specify mutually exclusive
> process instantiation conditions. If the PCS finds template configurations at startup that would
> guaranteed lead to the creation of processes of different templates for one message, it aborts the startup
> with an exception and an explanatory error log entry. The background is that the process ID has to be
> globally unique in the PCS.

### Correlation providers

By default, the attribute `processId` of a message is used to assign it to a process. Alternatively a
correlation provider can extract one or more process IDs from a message.

Single-instance tasks, which occur exactly once in a process, can be correlated to a message unambiguously
via the name of the message. Dynamically instantiated tasks need a correlation provider that extracts a task
instance ID from a message, so that the message can be assigned to a specific task. The message completion
condition uses the extracted origin task ID to mark tasks as completed if the name of the message matches
**and** the origin task ID matches.

A correlation provider provides 0..n process IDs and 0..n origin task IDs per message:

```java
public interface MessageCorrelationProvider<M extends Message> {

    Set<String> getOriginProcessIds(M message, ProcessCorrelationRepository processCorrelationRepository);

    Set<String> getRelatedOriginTaskIds(M message);
}
```

### Correlation by process ID vs. by process data

Correlation providers usually extract the process ID directly from the message — from the message attribute
`processId`, from the message references or from the payload.

But what if a message has to be correlated that was created *before* the process instance existed? Or if the
publisher of the message does not know the process ID at all? This is where correlation based on **process
data** helps.

Two messages are involved: one message writes a kind of ID into the process data of a process instance, the
other references the same ID in its message data. Through a `correlatedBy` entry on the type of the second
message, that message is correlated with the process instance of the first one. **The order in which the two
messages are processed does not matter** — see [late correlation](architecture.md#late-correlation).

A message can be correlated with a process instance exactly when the message contains data under the
specified message data key and this data (value, role) is found exactly under the given process data key in
the process data of the instance.

### Payload extractors

To extract specific data from the payload of a message:

```java
public interface PayloadExtractor<E extends MessagePayload> {

    default Set<MessageData> getMessageData(E payload) {
        return Collections.emptySet();
    }
}
```

```java
public class JmeRaceMobileCheckpointPassedEventPayloadExtractor
        implements PayloadExtractor<JmeRaceMobileCheckpointPassedEventPayload> {

    @Override
    public Set<MessageData> getMessageData(JmeRaceMobileCheckpointPassedEventPayload payload) {
        return Set.of(
                new MessageData("taskId", payload.getTaskId()),
                new MessageData("state", payload.getState()),
                new MessageData("date", payload.getControlDate().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)));
    }
}
```

### Reference extractors

To extract specific data from the message references:

```java
public interface ReferenceExtractor<E extends MessageReferences> {

    default Set<MessageData> getMessageData(E references) {
        return Collections.emptySet();
    }
}
```

The extracted data is persisted as `MessageData` — a key, a value and optionally a role — and is available in
custom conditions through the process context.

## Process data

Process data enables the correlation of messages that only arise in the course of a process:

```json
"processData": [
  {
    "key": "processDataKey",
    "source": {
      "message": "SomeEventName",
      "messageDataKey": "someKeyName"
    }
  }
]
```

When a message named `SomeEventName` arrives, the PCS checks whether message data with the key `someKeyName`
exists. If so, its value and role are stored in the process data under the key `processDataKey`.

## Relation patterns

By observing domain events, the PCS records the relations established between business objects during a
process as triples (business object, relation type, business object) for traceability purposes.

The process template declares which relations between business objects are expected in the process:

```json
"relationSystemId": "ch.admin.race.RaceSys",
"relationPatterns": [
  {
    "predicateType": "ch.test.predicate.Knows",
    "subject": {
      "type": "ch.test.Subject",
      "selector": {
        "processDataKey": "subjectKey",
        "role": "RaceParticipant"
      }
    },
    "object": {
      "type": "ch.test.Object",
      "selector": {
        "processDataKey": "objectKey"
      }
    }
  }
]
```

An instance can provide a Spring bean implementing `RelationListener` to be notified about newly discovered
relations, for example to publish them as an event or command:

```java
public interface RelationListener {

    /**
     * Invoked when new relations have been added to a process
     */
    void relationsAdded(Collection<Relation> relations);
}
```

### Relation patterns with multiple process data keys

If more than one instance of a process data key exists, one relation per pair is created by default (the
cartesian product). This can produce relations that are not correct in the concrete use case. Consider four
process data instances referring to two different assessments:

| Key                    | Value | Comment                                    |
|------------------------|-------|--------------------------------------------|
| `assessmentId`         | `a1`  | First version of the assessment            |
| `assessmentId`         | `a2`  | Second version of the assessment           |
| `assessmentArtefactId` | `a1`  | Artefact of the first version              |
| `assessmentArtefactId` | `a2`  | Artefact of the second version             |

The cartesian product yields four relations, of which only two are correct (`a1`–`a1` and `a2`–`a2`). The
property `joinType` restricts which relations are created:

```json
"relationPatterns": [
  {
    "object": { },
    "subject": { },
    "predicateType": "ch.test.predicate.Knows",
    "joinType": "byValue"
  }
]
```

| Value     | Condition                                                            |
|-----------|----------------------------------------------------------------------|
| `byValue` | If the values of two process data instances are identical and not null. |
| `byRole`  | If the role values of two process data instances are identical and not null. |

In the example above, `joinType=byValue` produces exactly the two correct relations.

### Relation patterns with a feature flag

All ID types and predicates that a business application reports have to be registered beforehand in the
receiving system. If they are not registered yet, publishing relations produces errors. To support continuous
deployment, publishing relations can be activated per relation pattern with a feature flag:

```json
"relationPatterns": [
  {
    "predicateType": "ch.test.predicate.Knows",
    "featureFlag": "NAME_OF_FEATURE_FLAG_1"
  }
]
```

```yaml
togglz:
  features:
    NAME_OF_FEATURE_FLAG_1:
      enabled: true
    NAME_OF_FEATURE_FLAG_2:
      enabled: false
```

If the feature flag is not active at the time of processing, the relation is not published. As soon as it is
activated, new relations are sent. Relations processed before the activation are **not** published
retroactively. If no feature flag is configured, the relations are always published.

The PCS exposes the state of the feature flags as a metric:

```
# HELP feature_flag Feature Flags
# TYPE feature_flag gauge
feature_flag{client="my-service",name="NAME_OF_FEATURE_FLAG_1"} 1.0
feature_flag{client="my-service",name="NAME_OF_FEATURE_FLAG_2"} 0.0
```

## Process relations

Relations between different process instances can be displayed in the UI. Which processes are related is
declared in the template:

```json
"processRelationPatterns": [
  {
    "name": "aName",
    "roleType": "origin",
    "originRole": "fallback text",
    "targetRole": "fallback text",
    "visibility": "both",
    "source": {
      "messageName": "SomeMessage",
      "messageDataKey": "relatedProcessId"
    }
  }
]
```

- `roleType`: `origin` or `target`
- `originRole`: relevant if the relation role is `target` or `both`
- `targetRole`: relevant if the relation role is `origin` or `both`
- `visibility`: `origin`, `target` or `both`

The following rules apply:

- the `name` must be unique per process relation pattern (it is used for internationalization),
- the message declared under `source.messageName` must also be declared in the `messages` block.

When a message is correlated with the process instance, the PCS checks whether the message is declared as the
source of a process relation pattern. If so, the message is expected to contain the declared
`messageDataKey`, and its value is stored in the field `relatedProcessId` of the `ProcessRelation` entity,
together with the role type, visibility and roles from the template.

## Process completion

When a process is created it is in the state `started`. When certain conditions are fulfilled it changes to
`completed`. Three attributes are determined on completion:

| Attribute     | Description            | Values                                                                                                                                                                 |
|---------------|------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `conclusion`  | Completion status      | `succeeded` — completed successfully; `cancelled` — cancelled in a controlled way, participating systems in a consistent state; `aborted` — aborted in an uncontrolled way, participating systems possibly inconsistent |
| `name`        | Name of the completion | String. Keys the translation of the human-readable completion reason, see [Internationalization](#internationalization). |
| `completedAt` | Completion timestamp   | Determined by the PCS                                                                                                                                                   |

More than one completion condition can be configured.

### Completion by a message

```json
"completions": [
  {
    "completedBy": {
      "message": "JmeRaceWeatherAlertActivatedEvent",
      "conclusion": "aborted",
      "name": "raceWeatherAlertAborted"
    }
  }
]
```

### Completion by a custom condition

```json
"completions": [
  {
    "completedBy": {
      "condition": "ch.admin.bit.jeap.jme.processcontext.condition.TooManyMaintenanceStopsProcessCompletionCondition"
    }
  }
]
```

```java
public interface ProcessCompletionCondition {

    ProcessCompletionConditionResult isProcessCompleted(ProcessContext processContext);
}
```

A custom condition decides, based on the process context, whether the process is completed. If it is not,
`ProcessCompletionConditionResult.IN_PROGRESS` has to be returned; otherwise the matching conclusion and
completion name:

```java
@Override
public ProcessCompletionConditionResult isProcessCompleted(ProcessContext processContext) {
    if (isCompleted) {
        return ProcessCompletionConditionResult.completedBuilder()
                .conclusion(conclusion)
                .name("myCompletionName")
                .build();
    } else {
        return ProcessCompletionConditionResult.IN_PROGRESS;
    }
}
```

#### Accessing messages in a condition

To access the messages of a process instance in a condition, use the query methods of
`ProcessContextMessageQueryRepository`, which `ProcessContext` inherits.

**Always use the most specific query method possible.** These methods issue database queries optimised for
the specific use case, so the most specific method gives the best performance:

- to check for the existence of a message type regardless of the actual count, use a `contains…()` method
  instead of a `count…()` method,
- to check for the existence of a certain message data key/value, use a `contains…()` method instead of
  loading all message data.

```java
public class TooManyMaintenanceStopsProcessCompletionCondition implements ProcessCompletionCondition {

    @Override
    public ProcessCompletionConditionResult isProcessCompleted(ProcessContext processContext) {
        if (processContext.containsMessageOfType("JmeRaceCarMaintenanceRequiredEvent")) {
            return ProcessCompletionConditionResult.completedBuilder()
                    .conclusion(ProcessCompletionConclusion.CANCELLED)
                    .name("tooManyMaintenanceStopsProcessCompletionCondition")
                    .build();
        } else {
            return ProcessCompletionConditionResult.IN_PROGRESS;
        }
    }
}
```

The available query methods:

```java
// get message data
processContext.getMessageDataForMessageType("JmeDocumentReviewedEvent");

// contains
processContext.containsMessageOfType("JmeDocumentReviewedEvent");
processContext.containsMessageByTypeWithAnyMessageDataKeyValue("JmeDocumentReviewedEvent",
        Map.of("anyKey", Set.of("anyValue", "anotherValue")));
processContext.containsMessageByTypeWithAnyMessageDataValue("JmeDocumentReviewedEvent",
        "anyKey", Set.of("anyValue", "anotherValue"));
processContext.containsMessageByTypeWithMessageData("JmeDocumentReviewedEvent", "anyKey", "anyValue");

// count
processContext.countMessagesByType("JmeDocumentReviewedEvent");
processContext.countMessagesByTypes(Set.of("JmeDocumentReviewedEvent"));
processContext.countMessagesByTypeWithAnyMessageData("JmeDocumentReviewedEvent", Map.of("anyKey", "anyValue"));
processContext.countMessagesByTypeWithMessageData("JmeDocumentReviewedEvent", "anyKey", "anyValue");

// tasks
boolean allTasksInFinalState = processContext.areAllTasksInFinalState();
```

### Completion when all tasks are completed

The predefined condition `AllTasksInFinalStateProcessCompletionCondition` completes a process when all its
tasks have been completed:

```json
"completions": [
  {
    "completedBy": {
      "condition": "ch.admin.bit.jeap.processcontext.plugin.api.condition.AllTasksInFinalStateProcessCompletionCondition"
    }
  }
]
```

### Default completion

If the `completions` attribute is not defined in a template, a process counts as completed when all tasks
that have to be planned have been planned, and all planned tasks have been completed.

## Process snapshots

The PCS can record the current state of a process in a **snapshot** when a certain condition is fulfilled for
the first time. Several snapshots can be created over the course of a process instance; they are numbered
with ascending integer version numbers. The creation of a snapshot is announced by publishing a
`ProcessSnapshotCreatedEvent`.

The snapshots are stored in an S3 object storage and kept for a configured number of days, see
[Configuration](configuration.md#process-snapshots). They are made available over a REST interface compatible
with the archive data interface of the Process Archive Service, so that they can be archived by a PAS
instance.

```json
"snapshots": [
  {
    "createdOn": {
      "completion": "any"
    }
  }
]
```

Instead of `any`, a specific final state can be given so that the snapshot is only created when that state is
reached: `succeeded`, `cancelled` or `aborted`.

### REST interface for process snapshots

| Method | Path                            | Parameters          | Auth                                                                                        | Response                                                     |
|--------|---------------------------------|---------------------|---------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| GET    | `/api/snapshot/{processOriginId}` | `version` (optional) | jEAP OAuth2 bearer token with the semantic role `system: <system>`, `resource: processsnapshot`, `operation: view` | `200` with the binary Avro representation (`avro/binary`), `403` denied, `404` version not found |

Without a version parameter, the newest available snapshot version is returned.

For a PAS instance to be able to validate the archive data objects, the process snapshot schema defined by
the PCS has to be added to the archive type registry of the business application. Make sure that the snapshot
schema versions matching the currently and previously used PCS versions are known to the PAS instance.

## Provided implementations

The plugin API ships ready-made implementations that can be referenced from a template instead of writing an
own class:

| Class                                                | Purpose                                                                                       |
|------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| `MessageProcessIdCorrelationProvider`                | Correlates a message via its `processId` attribute — the default behaviour.                    |
| `AlwaysProcessInstantiationCondition`                | Always instantiates the process, equivalent to `"triggersProcessInstantiation": true`.         |
| `NeverProcessInstantiationCondition`                 | Never instantiates the process.                                                                |
| `AllTasksInFinalStateProcessCompletionCondition`     | Completes the process when all its tasks have reached a final state.                           |
| `EmptySetPayloadExtractor` / `EmptySetReferenceExtractor` | Extract nothing; useful as an explicit no-op.                                             |
| `LoggingRelationListener`                            | Logs newly discovered relations — useful for development and for verifying relation patterns.  |

### Testing custom conditions

`ProcessContextStub` (in `jeap-process-context-plugin-api`, package `…plugin.api.context.test`) builds a
`ProcessContext` from a list of messages, so custom conditions can be unit tested without a database or a
running application:

```java
ProcessContext processContext = ProcessContextStub.builder()
        .originProcessId("origin-process-id")
        .processName("raceProcess")
        .messages(List.of(message))
        .allTasksCompleted(true)
        .build();

assertThat(new MyCompletionCondition().isProcessCompleted(processContext).isCompleted()).isTrue();
```

## Internationalization

The following elements of a process are displayed in the UI and have to be translated:

- the name of the template,
- the names of the tasks,
- the reasons of process completions,
- the origin and target roles of relations,
- the keys of user data of messages,
- the keys of task data.

The translations (DE, FR, IT) are defined in the directory `process/messages`:

- `messages.properties` (DE)
- `messages_fr.properties` (FR)
- `messages_it.properties` (IT)

```properties
# Name of a process
<process-name>.label=Rennen durch die Schweiz

# Name of a task
<process-name>.task.<task-name>=Rennen starten

# Key of a task data entry
<process-name>.task.<task-name>.data.<key>=Treibstofftyp

# User data of messages
userData.<key-name>=Vorname

# Reason of a process completion
<process-name>.completion.<completion-name>=Sofortiger Abbruch des Rennens wegen Wetterwarnung

# Origin and target roles of process relations
<process-name>.<relation-name>.originRole=Eine Beschreibung für die Origin Rolle
<process-name>.<relation-name>.targetRole=Eine Beschreibung für die Target Rolle
```

The completion name is either defined in the template:

```json
"completedBy": {
  "message": "JmeRaceWeatherAlertActivatedEvent",
  "conclusion": "aborted",
  "name": "raceWeatherAlertAborted"
}
```

or in the Java code, if a programmatic completion condition is used:

```java
return ProcessCompletionConditionResult.completedBuilder()
        .conclusion(ProcessCompletionConclusion.CANCELLED)
        .name("tooManyMaintenanceStopsProcessCompletionCondition")
        .build();
```

### Default translations

Default translations for general elements are predefined and do not have to be defined again in the
instance — they can be overridden, though. They are defined in
`jeap-process-context-domain/src/main/resources/default-messages/messages.properties`:

```properties
# Default completion conditions defined in jeap-process-context for all processes
completion.allTasksInFinalStateProcessCompletionCondition=Alle Prozessaufgaben haben einen Endzustand erreicht
completion.legacyProcessCompletionCondition=Alle Aufgaben wurden geplant und zu einem endgültigen Stand gebracht

# Default message user data
userData.id=Benutzer-ID
userData.familyName=Nachname
userData.givenName=Vorname
userData.businessPartnerName=Geschäftspartner
userData.businessPartnerId=Geschäftspartner-ID
```

## Changing existing templates

The hash of a process template is persisted as soon as a process instance is created. If a template is
changed, a migration takes place, see [Template Migration](template-migration.md).
