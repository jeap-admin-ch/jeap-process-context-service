# Configuration

All properties of the Process Context Service (PCS) are namespaced under `jeap.processcontext.*`. Baseline
defaults are defined in `processContextDefaultProperties.properties` of the service library. See
[Getting Started](getting-started.md#4-configure-the-application) for a minimal configuration of an instance.

## Kafka

| Property                                                     | Description                                                                                                    |
|--------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| `jeap.processcontext.kafka.topic.process-outdated-internal`  | Internal topic controlling the maximum internal parallelism of the PCS. Required.                              |
| `jeap.processcontext.kafka.topic.process-snapshot-created`   | Topic on which the creation of a process snapshot is announced. Required only if snapshots are configured.     |
| `jeap.processcontext.kafka.topic.add-process-data-command`   | Topic used to dispatch process-data backfill commands. Required when maintenance is enabled.                    |
| `jeap.processcontext.kafka.message-consumer-paused`          | Stops message consumption, e.g. while performing a maintenance operation. Default: `false`.                     |
| `jeap.processcontext.kafka.filters.<MessageType>`            | A [message filter](#message-filters) for the given message type.                                               |
| `jeap.processcontext.kafka.topic-check`                      | Whether the existence of the configured topics is checked at startup. Default: `true`, inactive in the `local` profile. |

The topics carrying the business messages are declared per message in the process template, together with the
optional `clusterName` — the PCS supports reading from multiple Kafka clusters. The internal messages of the
PCS are always sent and received on the default cluster.

The consumer parallelism is configured with the standard Spring property:

```yaml
spring:
  kafka:
    listener:
      concurrency: 3
```

### Message filters

Not all instances of a given message type are relevant for the PCS of a business application — events of
shared services in particular may originate from a different application. Messages that do not fulfil the
filter criterion are ignored by the PCS.

Filtering is meant to apply centrally to the whole PCS rather than per template, so message filters are
defined in the application configuration instead of in the templates:

```yaml
jeap:
  processcontext:
    kafka:
      filters:
        JmeRaceStartedEvent: ch.admin.bit.jeap.jme.processcontext.event.JmeRaceStartedEventMessageFilter
        OtherEvent: ch.admin.bit.OtherEventMessageFilter
```

Every filter implements `MessageFilter`:

```java
public interface MessageFilter<M extends Message> {

    /**
     * @return true if the message should be processed, false to ignore it
     */
    boolean filter(M message);
}
```

```java
@Slf4j
public class JmeRaceStartedEventMessageFilter implements MessageFilter<JmeRaceStartedEvent> {

    @Override
    public boolean filter(JmeRaceStartedEvent message) {
        var reference = message.getReferences().getWeatherAlertSubjectReference();
        if (reference != null && reference.getWeatherAlertSubject().toLowerCase().contains("filter")) {
            log.info("WeatherAlertSubjectReference '{}' contains 'filter': ignoring message", reference);
            return false;
        }
        return true;
    }
}
```

## Process templates

| Property                                              | Description                                                                     |
|-------------------------------------------------------|---------------------------------------------------------------------------------|
| `jeap.processcontext.template.classpath-location-pattern` | Where to load the process templates from. Default: `classpath:/process/templates/*.json`. |

### Template migration scheduler

Changed templates are migrated on the fly when a message is processed for a process instance, and
periodically in batches by a scheduler. See [Template Migration](template-migration.md) for the migration
rules. The values below are the defaults:

```yaml
jeap:
  processcontext:
    template:
      migration:
        lock-at-least: PT1M          # Minimal time to keep a lock at the migration triggering job
        lock-at-most: PT60M          # Max time to keep a lock at the migration triggering job
        batch-size: 500              # How many process instances to migrate at most in one batch
                                     # (only considers non-completed process instances)
        max-created-at-age-days: 180 # How old a process instance may be at most to be considered
        cron-expression: 0 10 * * * * # How often to run the migration scheduler. Default: at :10 past every hour
```

Configure the scheduler so that the migration events can be processed within the time between two runs.
Otherwise, multiple migration events are triggered for instances that have not been migrated yet.

## Housekeeping

The PCS automatically deletes old data from the database. Deleting a process instance always includes its
process updates and messages.

| Property (`jeap.processcontext.housekeeping.*`) | Default        | Description                                                                                 |
|-------------------------------------------------|----------------|---------------------------------------------------------------------------------------------|
| `cron-expression`                               | `0 20 0 * * *` | The housekeeping job runs daily at 00:20.                                                   |
| `lock-at-least`                                 | 5 seconds      | Minimal time to keep the lock for this job.                                                 |
| `lock-at-most`                                  | 30 minutes     | Maximal time to keep the lock for this job.                                                 |
| `completed-process-instances-max-age`           | `P180D`        | Completed processes are deleted after 180 days. Note the duration syntax.                   |
| `started-process-instances-max-age`             | `P365D`        | Processes that are not completed are deleted after 365 days. Note the duration syntax.      |
| `events-max-age`                                | `P90D`         | Messages not referenced by a process instance are deleted after 90 days.                    |
| `completed-maintenance-jobs-max-age`            | `P30D`         | Completed maintenance jobs and their tasks are deleted after 30 days.                       |
| `page-size`                                     | 500            | Number of records deleted at once.                                                          |
| `max-pages`                                     | 100000         | Max. pages to housekeep in one run, limiting the time one run can spend.                    |

## Process snapshots

Configured under `jeap.processcontext.objectstorage`:

| Property                  | Description                                          | Default | Optional            |
|---------------------------|------------------------------------------------------|---------|---------------------|
| `snapshot-bucket`         | Name of the bucket the snapshots are stored in.      |         | no                  |
| `snapshot-retention-days` | Number of days the snapshots are kept.               | 3       | yes                 |

| Property                                                              | Description                                                                     | Default |
|-----------------------------------------------------------------------|---------------------------------------------------------------------------------|---------|
| `jeap.processcontext.process-snapshot-language`                       | Language of the labels written into a process snapshot (`DE`, `FR`, `IT`).      | `DE`    |
| `jeap.processcontext.process-snapshot-archive-retention-period-months`| Retention period declared in the archive data of a snapshot, in months.         | 60      |

If no snapshot bucket name is configured, the snapshot feature is inactive. If the PCS then finds a process
template configuring snapshots at startup, it aborts the startup with an exception.

The connection to the S3 storage is configured under `jeap.processcontext.objectstorage.connection`:

| Property     | Description                                | Default      |
|--------------|--------------------------------------------|--------------|
| `access-url` | URL for accessing the S3 storage.          |              |
| `region`     | Region of the S3 storage.                  | `aws-global` |
| `access-key` | Access key for accessing the S3 storage.   |              |
| `secret-key` | Secret key for accessing the S3 storage.   |              |

Depending on the deployment environment, the access URL and the credentials may be provided by the
infrastructure instead of being configured explicitly. The PCS checks the access to the configured bucket at
startup and aborts with an exception if it is not possible.

## Frontend and OAuth

The PCS UI is secured with OAuth2/OIDC; the backend is a jEAP OAuth2 resource server
(`jeap.security.oauth2.resourceserver.*`). `jeap.processcontext.frontend.sts-server` and
`token-aware-pattern` default to the issuer of the configured authorization server and to the API path of the
application, so they usually do not have to be set.

```yaml
jeap:
  processcontext:
    frontend:
      client-id: "process-context"
      system-name: "jme"                    # system name used in the role names
      auto-login: true
      silent-renew: true
      renew-user-info-after-token-renew: true
      application-url: http://localhost:8303/process-context/
      logout-redirect-uri: http://localhost:8303/process-context/
      token-aware-pattern:
        - ^/process-context/api/.*
      mock-pams: false
      pams-environment: REF
  security:
    oauth2:
      resourceserver:
        system-name: "jme"
```

The required user role is described in [User Interface](user-interface.md#roles).

## Maintenance jobs

Maintenance job APIs are disabled by default. Enable them only for controlled operator access:

```yaml
jeap:
  processcontext:
    maintenance:
      enabled: true
      limits:
        max-tasks-per-job: 10000
        max-field-length: 2000
        max-process-data-values-per-task: 100
        max-process-data-values-per-job: 10000
        max-request-bytes: 10485760
        relation-reevaluation-page-size: 500
        max-relation-candidates-per-task: 100000
```

`max-tasks-per-job` limits the number of processes in a relation-reevaluation request or entries in a process-data
backfill request. `max-field-length` limits request strings by their UTF-8 byte length.
`max-process-data-values-per-task` limits the values in each backfill entry.
`max-process-data-values-per-job` limits the total values across a backfill request, and `max-request-bytes` limits the
YAML request size in bytes before full deserialization (10 MiB by default).

When enabled, the relation-reevaluation and process-data backfill APIs described under
[Maintenance jobs](maintenance.md) are available.

The endpoints use OAuth2 bearer-token authentication. Bearer-token requests are excluded from Spring Security's CSRF
matcher and therefore do not require a browser CSRF cookie/header pair.

Jobs, tasks, and their keyed `ProcessContextOutdatedEvent` or `AddProcessDataCommand` outbox messages are persisted
atomically. A backfill command atomically adds process data, queues its `ProcessContextOutdatedEvent`, and moves the task
to `event-queued`; the event reevaluates relations and completes the task. Messages are sent immediately after commit;
the outbox relay remains the delivery fallback. Relation candidates are selected from the database in pages, and
`max-relation-candidates-per-task` rejects unsafe Cartesian workloads before any relation is created. Missing processes
and processing failures are recorded as terminal task results and acknowledged; a message is retried only when its
terminal result cannot be persisted. PCS-internal maintenance messages are exempt from application contract validation,
so enabling maintenance does not require additional message contracts.

A job starts in `open` state. Relation-reevaluation tasks start in `event-queued`; process-data backfill tasks move from
`command-queued` to `event-queued`. A task remains `event-queued` until it atomically reaches a terminal state
(`succeeded`, `not-found`, or `failed`). Processing locks only the current task, so tasks for different processes in the
same job can run concurrently. The shared job is locked only after task processing to calculate its result from current
task counts. The job becomes `completed` when all tasks are terminal and records a `succeeded`, `partially-succeeded`,
or `failed` result. Completed jobs are removed by regular housekeeping after
`jeap.processcontext.housekeeping.completed-maintenance-jobs-max-age`, which defaults to `P30D`.

### PAMS and ePortal

The UI header contains the ePortal service navigation of Oblique, which is backed by PAMS
(`https://pams-api.eportal<environment>.admin.ch`).

| Property           | Description                                                                                                         | Default |
|--------------------|-----------------------------------------------------------------------------------------------------------------------|---------|
| `pams-enabled`     | Whether the application is integrated with PAMS/ePortal.                                                             | `true`  |
| `pams-environment` | ePortal environment of the service navigation: `DEV`, `TEST`, `REF`, `ABN` or `PROD`.                                | -       |
| `mock-pams`        | Treat the PAMS session as always active instead of reading it from the service navigation. Implied by `pams-enabled: false`. | `false` |

Set `pams-enabled: false` for deployments without PAMS:

```yaml
jeap:
  processcontext:
    frontend:
      pams-enabled: false
```

The UI then does not contact the ePortal backend at all — no service navigation requests, no ePortal session
timeout handling — and authentication is based solely on OAuth2/OIDC. The header controls served by PAMS
(login/logout, profile, messages, applications) would be non-functional and are hidden; the language
selection remains available.

Note that `pams-environment` must match the environment of the identity provider the UI authenticates
against. Pointing the service navigation at a different environment than the authentication leads to an
inconsistent login state in the header and to logout and timeout redirects into the wrong ePortal.

## Log deep link

The UI can link from the trace ID of a message directly into the log system. The query template must contain
the token `{traceId}`, which is replaced by the actual trace ID.

| Property                 | Description                       | Default                    |
|--------------------------|-----------------------------------|----------------------------|
| `log.deep-link.base-url` | Query template of the log system. | Splunk template            |

Example for AWS CloudWatch:

```yaml
log:
  deep-link:
    base-url: "https://<region>.console.aws.amazon.com/cloudwatch/home?region=<region>#logsV2:logs-insights$3FqueryDetail$3D~(end~0~start~-259200~timeType~'RELATIVE~unit~'seconds~editorString~'fields*20*40timestamp*2c*20*40message*2c*20*40log*0a*7c*20filter*20traceId*20*3d*20*22{traceId}*22*0a*7c*20sort*20*40timestamp*20desc*0a*7c*20limit*2020~source~(~'))"
```

## Encrypted Kafka records

The PCS can consume Kafka records encrypted with jeap-messaging / jeap-crypto. This requires the
`jeap-vault-starter` and `jeap-crypto-vault-starter` dependencies. The record carries a reference to the
wrapping key used, so usually only the Vault URL and the system name (`jeap.vault.system-name`) have to be
configured.
