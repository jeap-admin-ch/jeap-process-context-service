# Upgrading to PCS Version 17

Major version 17 of the PCS focused on improving performance and behaviour under heavy load, in particular on
being able to handle process instances with a large number of messages, tasks and relations. To achieve this,
some never-used or little-used features were removed or replaced.

> **The deployment procedure below is mandatory.** The new version drops some database tables, which requires
> a dry-out period to avoid data loss. The property `jeap.processcontext.release.min-version` was added to
> prevent accidental upgrades that skip these steps. It was removed again in version 20.2.0, after the
> migration period — so on newer versions the property no longer exists and can be dropped from the
> configuration.

## Removed features

- `ProcessInstanceCreated` and `ProcessInstanceCompleted` events
- Process milestones and the associated `MilestoneReachedEvent`s
- Process instance creation using
  - the REST API (deprecated for a long time)
  - the `CreateProcessInstanceCommand` — as far as known, all PCS instances instantiate processes using
    domain events
- Programmatic snapshot conditions were removed from the public API. Snapshots can still be created on
  process completion as before.

## Other notable changes

- The plugin API, namely the `ProcessContext` class passed to custom conditions, has been adapted:
  - it no longer provides full in-memory collections of messages; these have been replaced by query-style
    methods that avoid loading a large amount of data into memory. See `ProcessContextMessageQueryRepository`
    for the available methods — this interface is inherited by `ProcessContext`.
  - access to the list of tasks and to the current process state has been removed from `ProcessContext`.
    This was never used and enables internal optimisations.
- Deprecated classes in the PCS API module have been removed. This includes the types containing the term
  "Event", which were replaced by equivalents using the term "Message" a long time ago.
- Types from the package `event` have been moved to `message` in the PCS API.
- The property `jeap.processcontext.housekeeping.process-update-max-age` is no longer required, as the
  `process_update` table has been removed.
- Process template migrations are now executed periodically in batches instead of at PCS startup, which
  previously led to load peaks. See
  [Template migration scheduler](configuration.md#template-migration-scheduler).

## Upgrade procedure

| Step | Action                                                                                                                           | Explanation                                                                                                                                                                              |
|------|----------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1    | Set the properties `jeap.processcontext.kafka.message-consumer-paused: true` and `jeap.processcontext.release.min-version: 17`   | This stops message processing by the PCS; pending messages are processed later. The min-version property ensures no accidental upgrade to v17 happens without following this guide.       |
| 2    | Update the PCS to at least 16.3.x — but **not** to 17.x or higher                                                                | This ensures the PCS respects the `message-consumer-paused` flag. You may also do this step first and set the property afterwards; in that case restart the instances after the change.   |
| 3    | Wait for the lag on the topics referenced by `jeap.processcontext.kafka.topic.process-outdated-internal` and `jeap.processcontext.kafka.topic.process-changed-internal` to reach 0 | This ensures all data from the input topics has been persisted in the database and no message processing occurs during the upgrade.                                                        |
| 4    | Update to PCS 17.x or higher and wait for the deployment and startup of the instances                                            | This applies the database migrations. Always use the latest available version when upgrading.                                                                                            |
| 5    | Remove `jeap.processcontext.kafka.message-consumer-paused` or set it to `false`                                                  | Re-enables message consumption. Do **not** remove the min-version property; it will be removed in a later PCS version.                                                                    |
| 6    | Restart the PCS instances                                                                                                        | So that they pick up the changed property and resume message consumption.                                                                                                                |
| 7    | Cleanup                                                                                                                          | Delete the unused topics, see below.                                                                                                                                                     |

### Checking the lag on the internal topics

The consumer lag can be checked with the monitoring tooling of the deployment environment — for example a
Kafka dashboard in Grafana, or the consumer group tab of a Kafka console. Note that the topics may be named
differently in your instance; check the topic names in the PCS configuration properties
`jeap.processcontext.kafka.topic.process-outdated-internal` and
`jeap.processcontext.kafka.topic.process-changed-internal`.

For AWS MSK, the lag can be queried in CloudWatch:

```sql
SELECT SUM(SumOffsetLag)
FROM SCHEMA("AWS/Kafka", "Cluster Name", Topic, "Consumer Group")
WHERE "Cluster Name" = 'kafka' AND "Topic" = '<app>-process-state-changed'
GROUP BY Topic
ORDER BY SUM() DESC
LIMIT 500
```

## Code changes

### Custom conditions

Many custom conditions require access to the messages in the process context. As `ProcessContext` no longer
provides a list of messages, use the methods of `ProcessContextMessageQueryRepository`, which `ProcessContext`
inherits.

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

### Package renaming

Some PCS API classes were renamed from `.api.event` to `.api.message` to align the package name with the
contained classes:

```java
// was ...api.event.* for some types
import ch.admin.bit.jeap.processcontext.plugin.api.message.*;
```

### Removal of deprecated types

```java
// Correlation providers:
// DomainEventCorrelationProvider has been removed, use MessageCorrelationProvider instead
public class JmeRaceControlpointPassedEventCorrelationProvider
        implements MessageCorrelationProvider<JmeRaceControlpointPassedEvent> { // ...

// Reference and payload extractors:
// EventData has been removed, use MessageData instead
// getEventData has been removed, use getMessageData instead
public Set<MessageData> getMessageData(JmeRaceCarPostChecksCompletedEventPayload payload) { // ...
```

## Cleanup

After deploying PCS 17, the following topics can be removed in all Kafka environments — note that they may be
named differently in your instance, check the PCS configuration for the actual names:

- `jeap.processcontext.kafka.topic.process-changed-internal`
- `jeap.processcontext.kafka.topic.process-instance-created`
- `jeap.processcontext.kafka.topic.process-instance-completed`
- `jeap.processcontext.kafka.topic.create-process-instance`

The corresponding configuration properties can be removed as well, as they no longer have an effect in PCS 17.

If the long-removed topics `<app>-process-task-completed` and `<app>-process-task-planned` are still present,
they can be removed too.
