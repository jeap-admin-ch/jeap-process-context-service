# Configuration

All properties of the Process Context Service (PCS) are namespaced under `jeap.processcontext.*`. Baseline
defaults are defined in `processContextDefaultProperties.properties` of the service library. See
[Getting Started](getting-started.md#4-configure-the-application) for an instance configuration excerpt and its deployment prerequisites.

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

The PCS automatically deletes old data from the database. Deleting a process instance removes its
message references, tasks, process data and relations. The messages themselves are cleaned up separately
once they are old enough and no process instance references them; a message can belong to more than
one process instance.

The age limits for both completed and started process instances are measured from instance creation
(`createdAt`), not from completion or the most recent update. A long-running instance can therefore
become eligible for cleanup immediately after completion if it already exceeds the completed-instance
age limit.

| Property (`jeap.processcontext.housekeeping.*`) | Default        | Description                                                                                 |
|-------------------------------------------------|----------------|---------------------------------------------------------------------------------------------|
| `cron-expression`                               | `0 20 0 * * *` | The housekeeping job runs daily at 00:20.                                                   |
| `lock-at-least`                                 | 5 seconds      | Minimal time to keep the lock for this job.                                                 |
| `lock-at-most`                                  | 30 minutes     | Maximal time to keep the lock for this job.                                                 |
| `completed-process-instances-max-age`           | `P180D`        | Completed processes created more than 180 days ago are deleted. Note the duration syntax.                   |
| `started-process-instances-max-age`             | `P365D`        | Non-completed processes created more than 365 days ago are deleted. Note the duration syntax.      |
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

The archive-retention-months value is exposed as snapshot metadata and a REST response header.
It does not configure the current PAS archive type's retention. See
[Snapshot REST integration](process-templates.md#rest-interface-for-process-snapshots) for version
selection and the separate PCS/PAS retention settings.

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

### PAMS and ePortal

The UI uses Oblique's ePortal service navigation. See the shared
[PAMS and ePortal guide](https://jeap-admin-ch.github.io/docs/concepts/frontend/pams-and-eportal)
for integration responsibilities and environment alignment.

Configure this service under `jeap.processcontext.frontend`:

| Property suffix | Meaning | Default |
| --- | --- | --- |
| `pams-enabled` | Enable PAMS/ePortal integration. | `true` |
| `pams-environment` | Service-navigation environment: `DEV`, `TEST`, `REF`, `ABN` or `PROD`. Configure for the target environment. | None |
| `mock-pams` | Treat the PAMS session as active instead of reading its state from the service navigation. Also implied by `pams-enabled: false`. | `false` |

The selected environment determines the portal endpoint used by the service navigation. Select the
environment matching the UI's identity provider; do not construct endpoint names from the enum values.

For a deployment without PAMS:

```yaml
jeap:
  processcontext:
    frontend:
      pams-enabled: false
```

This disables ePortal requests and PAMS session-timeout handling. Authentication uses OAuth2/OIDC;
PAMS header controls (login/logout, profile, messages and applications) are hidden, while language
selection remains available. `mock-pams` only mocks session state; use `pams-enabled` to disable
portal integration.

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

`max-tasks-per-job` limits the number of processes in a relation-reevaluation request, entries in a process-data
backfill request, or relation IDs in a relation-republication request. `max-field-length` limits request strings by
their UTF-8 byte length.
`max-process-data-values-per-task` limits the values in each backfill entry.
`max-process-data-values-per-job` limits the total values across a backfill request, and `max-request-bytes` limits the
YAML request size in bytes before full deserialization (10 MiB by default).

`relation-reevaluation-page-size` controls database paging of relation candidates.
`max-relation-candidates-per-task` rejects excessive Cartesian workloads before relations are created.

For required topics, dependencies, authorization, task states and delivery semantics, see
[Maintenance jobs](maintenance-jobs.md). Completed-job retention is configured under
[Housekeeping](#housekeeping).

## Log deep link

The UI opens the log system from a message's trace ID.

| Property | Meaning |
| --- | --- |
| `log.deep-link.base-url` | Log query URL template containing the literal `{traceId}` token, replaced by the UI. |

Set the URL for the deployment's logging environment. The shared
[Log deep links guide](https://jeap-admin-ch.github.io/docs/concepts/logging/log-deep-links)
explains the approach and provides Splunk and CloudWatch query examples.

## Encrypted Kafka records

The PCS can consume encrypted Kafka records through jEAP Messaging and jEAP Crypto. The record carries
information about the wrapping key used for encryption. Configure access to the corresponding key
management system in the PCS instance. See [Encrypting messages](https://jeap-admin-ch.github.io/docs/building-blocks/libraries/jeap-messaging/encrypting-messages)
for the supported integration, dependencies and key configuration; there is no PCS-specific decryption API.
