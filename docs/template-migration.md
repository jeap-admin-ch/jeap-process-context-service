# Template Migration

The hash of a process template is persisted as soon as a process instance is created. If a template is
changed afterwards, the existing process instances are migrated.

## Context

The PCS records messages and assigns them to a process context, making the state of a process transparent.
Conditions can be formulated for when milestones are reached and when tasks are considered completed, which
in turn influences when the process itself is considered completed.

Processes are instantiated by the process origin, usually the service representing the starting point of the
process. The actual process logic lies in the services consuming and producing the messages assigned to the
process. Processes can be open for a long time until they are completed — usually days or weeks, i.e. across
deployments and releases of new features and process logic in those services.

Pure recording can happen relatively independently of the evolution of the process over time. Logic for
reaching milestones and completing tasks, and therefore the process, is more tightly coupled to the
occurrence of expected messages and thus to the flow of the process as defined in the template.

## Constraints

| #  | Constraint                            | Description                                                                                                                                                                                                                                    |
|----|---------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| R1 | Recording                             | The PCS records. It cannot intervene in or control the actual process flow, it only represents it. The process flow is determined solely by the current logic of the services participating in the process. Planning dynamic tasks is likewise the responsibility of those services. |
| R2 | "There is no process"                 | It follows that there is always exactly one productive version of a process — unless the participating services implement some kind of migration logic themselves. One could also argue that there is no process at all, only instances of connected message streams. |
| R3 | Changes in business logic and message flow are a fact | That process flows change over time while processes are still open is a fact determined by the participating services and their business logic. The genuinely difficult part of migration therefore lies in those services: they have to make sure the changed message flow still works with already instantiated flows. The PCS perceives the symptom — the observed process no longer matches the template that was active when the process was instantiated — and has to cope with the template changing. |
| R4 | Done means done                       | Completed processes stay completed and remain untouched by changed templates. The same applies to completed tasks.                                                                                                                              |

## Automated migration rules

These rules are applied automatically by the PCS after a template has been changed — time based in batch
mode, or as soon as a message is received for a process instance. See
[Template migration scheduler](configuration.md#template-migration-scheduler) for the configuration.

In general: for process instances in a non-final state, a re-evaluation of the state is always triggered when
the template has changed, so that new definitions (new process data etc.) are applied.

### Tasks

| Change in the process template | Task instance state | Migration rule                             |
|--------------------------------|---------------------|--------------------------------------------|
| New dynamic task type          | –                   | Create a task instance in the state `UNKNOWN` |
| New single task type           | –                   | Create a task instance in the state `UNKNOWN` |
| Task type deleted              | Non-final state     | Change the task instance state to `DELETED` |
| Task type deleted              | Final state         | No action                                  |

### Messages

| Change in the process template | Migration rule                                                                          |
|--------------------------------|-------------------------------------------------------------------------------------------|
| New message reference          | No action — the message is consumed from now on                                          |
| Message reference deleted      | No action — the message is no longer consumed, already existing messages remain          |

### Message data

| Change in the process template  | Migration rule                                              |
|---------------------------------|-------------------------------------------------------------|
| New message data definition     | No action — the data is generated from now on               |
| Message data definition deleted | No action — existing message data remains                   |

### Process data

| Change in the process template  | Migration rule                                              |
|---------------------------------|-------------------------------------------------------------|
| New process data definition     | No action — the process data is generated from now on       |
| Process data definition deleted | No action — existing process data remains                   |

### Relations

| Change in the process template | Migration rule                                          |
|--------------------------------|---------------------------------------------------------|
| New relation pattern           | No action — the relation is created from now on         |
| Relation pattern deleted       | No action — existing relations remain                   |

## Specific migration rules

All elements created with the state `UNKNOWN` during the automated migration (tasks) have to be migrated with
a database script. This migration depends on the business context and has to be implemented by the project.
