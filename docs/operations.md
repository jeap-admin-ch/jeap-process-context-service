# Operations

## Scaling

The PCS has to be multi-instance capable: operating the application is the responsibility of the teams, and
it is not realistic to require that the service is never scaled and never deployed with zero downtime (which
requires at least temporary multi-instance operation).

The service scales with the number of instances and the number of partitions of the consumed topics, see
[Scaling and concurrency](getting-started.md#scaling-and-concurrency).

## Housekeeping

Old data is deleted automatically from the database by a scheduled job, see
[Configuration](configuration.md#housekeeping).

## Maintenance jobs

PCS provides maintenance jobs for relation reevaluation and process-data backfill. The APIs are disabled by default and
execute each process as a durable, independently tracked task. See [Maintenance jobs](maintenance.md) for enablement,
authorization, request and report formats, processing semantics, and failure handling.

## Metrics

The PCS publishes metrics that allow it to be monitored and analysed in operation.

### Counters

| Metric name                                | Dimensions        | Value                             |
|--------------------------------------------|-------------------|-----------------------------------|
| `jeap_pcs_process_created_instances_total` | `process_template`| Number of created processes       |
| `jeap_pcs_processes_completed_total`       | `process_template`| Number of completed processes     |
| `jeap_pcs_messages_received_total`         | `first_processing`| Number of received messages       |
| `jeap_pcs_process_updates_processed_total` |                   | Number of processed process updates |
| `jeap_pcs_failed_process_updates_total`    |                   | Number of failed process updates  |
| `jeap_pcs_snapshot_created_total`          | `process_template`| Number of created process snapshots |

### Timers

Timers define several timelines per metric name, with the following suffixes. In addition, a histogram bucket
is created for 50%, 80% and 99% of all measurements (`quantile` tag).

| Suffix           | Meaning                       |
|------------------|-------------------------------|
| `_seconds`       | Measured duration             |
| `_seconds_count` | Number of measured durations  |
| `_seconds_sum`   | Total of the measured durations |
| `_seconds_max`   | Maximum measured duration     |

| Metric name                                        | Measures                                                                                       |
|----------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `jeap_pcs_process_message`                         | Receiving a message                                                                             |
| `jeap_pcs_early_correlate_message`                 | Correlating a message to an origin process ID, either by correlation provider or by process data |
| `jeap_pcs_late_correlate_message`                  | Late correlation — correlating a message over newly created process data                        |
| `jeap_pcs_update_process_state`                    | Processing an outdated event, including the process update processing                           |
| `jeap_pcs_handle_message_for_process_instance`     | Processing a single message as part of the update of a process instance                         |
| `jeap_pcs_process_update`                          | Updating the state of a process instance, including task creation                               |
| `jeap_pcs_handle_pending_messages`                 | Finding and handling pending messages when a new process instance is created                     |
| `jeap_pcs_update_migrate`                          | Processing an event migrating a process instance because of a changed template                  |
| `jeap_pcs_migration_trigger`                       | Triggering the process instance migration for modified templates                                |
| `jeap_pcs_housekeeping_cleanup`                    | Duration of a housekeeping run                                                                  |
| `jeap_pcs_create_snapshot`                         | Creating a process snapshot                                                                     |
| `jeap_pcs_produce_process_snapshot_created_event`  | Producing a `ProcessSnapshotCreatedEvent`                                                       |
| `jeap_pcs_relation_service_new_process_data`       | Deriving new relations from newly created process data                                          |
| `jeap_pcs_relation_service_notify_listeners`       | Notifying the registered `RelationListener`s about new relations                                |
| `jeap_pcs_s3_client_*`                             | S3 client operations for snapshots (`put_object`, `get_object`, `head_bucket`, `list_objects`, and the bucket lifecycle configuration) |
| `jeap_pcs_repository_*`                            | Repository and query timers for database access, per entity (`processinstance`, `message`, `taskinstance`, `processdata`, `relation`, …) |

In addition, the state of the [feature flags](process-templates.md#relation-patterns-with-a-feature-flag) is
published as the `feature_flag` gauge.

## Error handling

Errors occurring while processing a message are handled by the Error Handling Service. Transient data access
and retryable Kafka exceptions are classified as temporary, everything else as permanent, see
[Error handling](architecture.md#error-handling).
