# User Interface

The PCS bundles an Angular UI that visualises the state of the process instances and their tasks for business
users. It is served by the application itself under the configured servlet context path, e.g.
`/process-context`.

## Views

- **Start page** — lists and filters the process instances, and opens a process instance by its origin
  process ID.
- **Process view** — shows a process instance with its tasks as a checklist, together with the messages
  correlated to the instance, the process data, the message data, the relations between business objects and
  the relations to other process instances.

The labels of processes, tasks, task data, completion reasons and relation roles come from the translations
of the process template, see
[Internationalization](process-templates.md#internationalization).

## Roles

| Semantic role                     | Usage                                            |
|-----------------------------------|--------------------------------------------------|
| `<system>_@processinstance_#view` | Required to open the UI and read the REST API.   |
| `<system>_@processsnapshot_#view` | Required to read process snapshots over the REST API. |

The system name has to be configured in `jeap.security.oauth2.resourceserver.system-name` and
`jeap.processcontext.frontend.system-name`.

## Deep links

| View name              | `ProcessInstanceById`                                                                                                     |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| **Path**               | `/views/process-instance-by-id`                                                                                            |
| **Description**        | Opens the process instance referenced by `originProcessId`. If the referenced instance is not found, the user is redirected to the start page. |
| **Available from**     | 5.9.0                                                                                                                       |

| Parameter         | Type/format   | Optional | Description                                |
|-------------------|---------------|----------|--------------------------------------------|
| `originProcessId` | `string/uuid` | No       | The origin process ID of the process instance |

## Trace ID and link to the log system

The trace ID of the messages can be inspected in the UI, which simplifies debugging and improves
traceability. Next to the trace ID, a button links into the log system (Splunk or AWS CloudWatch) to show the
relevant log entries. The link is configured with `log.deep-link.base-url`, see
[Configuration](configuration.md#log-deep-link).
