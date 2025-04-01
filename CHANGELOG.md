# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [13.3.0] - 2025-04-01

### Changed

- Messages and tasks in process detail UI are now sorted by message creation time instead of reception time

## [13.2.2] - 2025-04-01

### Changed

- Update parent from 26.35.0 to 26.41.3 (update was missing in last version)

## [13.2.1] - 2025-03-28

### Changed

- Update parent from 26.35.0 to 26.41.3

## [13.2.0] - 2025-03-07

### Changed

- Configure proxy to work around the issue https://github.com/aws/aws-sdk-java-v2/issues/4728 which is coming with the aws sdk update
- Update parent from 26.33.0 to 26.35.0
 
## [13.1.0] - 2025-03-06

### Changed

- Update parent from 26.26.0 to 26.33.0

## [13.0.0] - 2025-02-26

### Changed
- **BREAKING** - Removed default values for the following Spring properties:
  - `jeap.processcontext.frontend.logoutRedirectUri`

## [12.2.0] - 2025-02-26

### Added

- Added @bbc/apache2-license-checker for automated license checking at build time.
- Use @license-checker instead of @generate-license-file as license file generator package.

## [12.1.1] - 2025-02-19

### Changed

- Upgraded to jeap-spring-boot-parent version 26.26.0.

## [12.1.0] - 2025-02-18

### Changed

- Performance optimization of the housekeeping by dropping fk constraints to the `events` table
- Dropped foreign key constraint `events_event_data_events_id_fkey` from the table `events_event_data`.
- Dropped foreign key constraint `events_origin_task_ids_events_id_fkey` from the table `events_origin_task_ids`.
- Dropped foreign key constraint `events_user_data_events_id_fkey` from the table `events_user_data`.
- Dropped foreign key constraint `task_instance_planned_by_fkey` from the table `task_instance`.
- Dropped foreign key constraint `task_instance_completed_by_fkey` from the table `task_instance`.
- Changed the default batch size for the housekeeping queries [pages] from `100` to `500`

## [12.0.0] - 2025-02-12

### Changed
- Breaking: Removed the default value for the config property 'log.deep-link.base-url'. Add the property to your application
  configuration to enable links into your specific log aggregation system.
- Updated jeap parent and third party libraries to latest versions.
- Fixed SonarQube bug findings and security hotspots.
- Prepared the project for publication as open source on GitHub.
- Disable license plugins for service instances

## [11.1.1] - 2025-01-14

### Added

- Added the cycle mode to events_event_data_id_seq and events_origin_task_ids_id_seq, so that they will restart from the beginning after reaching its maximum value.

## [11.1.0] - 2025-01-14

### Changed

- Added the module jeap-process-context-service-instance which will instantiate a jeap process context service instance when used as parent project.

## [11.0.0] - 2025-01-09

### Changed

- Messages can no longer be consumed without a valid contract.
- At startup, the PCS validates contracts against the template to ensure that all necessary contracts are present. Otherwise, the startup will fail. 

## [10.8.1] - 2025-01-07

### Changed

- Update parent from 26.22.1 to 26.22.2

## [10.8.0] - 2025-01-06

### Changed

- Update parent from 26.21.1 to 26.22.1

## [10.7.0] - 2025-01-06

### Changed

- default value (3 days) for snapshot-retention-days removed
- the configuration (`jeap.processcontext.objectstorage.snapshot-retention-days`) is required if a snapshot bucket is defined

## [10.6.0] - 2024-12-19

### Changed

- Update parent from 26.16.0 to 26.21.1

## [10.5.0] - 2024-11-21

### Changed

- Migrated Header widget to Oblique Service Navigation

## [10.4.0] - 2024-11-19

### Changed

- The PCS no longer creates tasks in the state NOT_PLANNED at process start.
- Update to jeap-spring-boot-parent version 26.16.0

## [10.3.2] - 2024-11-18

### Changed

- Fixed builder and changed to angular-devkit/build-angular:browser.

## [10.3.1] - 2024-11-15

### Changed

- Fix 'Cypress could not verify that this server is running' error with baseUrl in component test after updating to v13. 
- See also: https://github.com/cypress-io/cypress/issues/27990

## [10.3.0] - 2024-11-08

### Changed

- Update to @angular/core@18 @angular/cli@18
- Update @oblique/oblique to v12.0.1
- Migrated standalone components to non-stnadalone to support Oblique v12
- Updated Cypress to v.13.15.2

### Fixed

- CVE-2024-39338 @Axios 1.6.8
- CVE-2024-4068 @Braces 3.0.2

## [10.2.0] - 2024-10-31

### Changed

- Update parent from 26.4.0 to 26.5.0

## [10.1.0] - 2024-10-17

### Changed

- Update parent from 26.3.0 to 26.4.0

## [10.0.1] - 2024-10-16

### Fixed
- UI: On process reload the task template view gets resettet. 

## [10.0.0] - 2024-10-10

### Changed
- BREAKING: removed support of hardcoded translations in process definitions
    - Template label, task label and process completion reason removed from process template definition
    - Process completion: reason removed from database (use message translation instead)
    - Process completion: name is now required in order to retrieve the according translation

### Added
- Startup check of required translations
- Default translations for standard user data and process completions

## [9.4.0] - 2024-10-09

### Changed
- Extended process context snapshots to contain user data and task data in ProcessSnapshot archive type version 2
  which is fully compatible to version 1. 

## [9.3.0] - 2024-10-01

### Added
- New UI: Task Details

## [9.2.0] - 2024-09-30

### Added
- Added support for task data in the backend

## [9.1.0] - 2024-09-20

### Changed

- Update parent from 26.0.0 to 26.3.0

## [9.0.1] - 2024-09-17

### Fixed
- Changed snapshot archive data type version of snapshots written to S3 from "1.0.0" to the correct "1". 
- Set the snapshotVersion as reference in ProcessSnapshotCreatedEvent

## [9.0.0] - 2024-09-16

### Changed

- Breaking: Removed support of custom task completion conditions: tasks can only be completed by domain events
- Store the UserData of all received messages in the db
- When a task instance is started or completed, the linked message is stored in the instance

## [8.2.1] - 2024-09-12

### Changed

- Set the processId as reference in ProcessSnapshotCreatedEvent 

## [8.2.0] - 2024-09-06

### Changed

- Update parent from 25.4.0 to 26.0.0

## [8.1.0] - 2024-09-04

### Changed

-  Added id fields populated by sequences as primary keys to the message data and the origin task id tables, also
   made the id on the process relation table a primary key. This is needed by the AWS database migration service
   to be able to migrate a PCS database to AWS.

## [8.0.0] - 2024-08-23

### Changed

- Breaking: Removed support of TaskPlanned and TaskCompleted events

## [7.24.0] - 2024-08-22

### Changed

- Update parent from 25.3.0 to 25.4.0

## [7.23.0] - 2024-08-13

### Changed

- Add new error page in case of forbidden access
- Update jeap-spring-boot-parent from 24.5.0 to 25.3.0

## [7.22.1] - 2024-07-18

### Changed

- Update com.google.guava to v.33.2.1-jre 

## [7.22.0] - 2024-07-16

### Changed

- Update parent from 24.4.0 to 24.5.0, including the upgrade to Spring Boot 3.3.1
- Metrics do not contain the word 'created' anymore, as it is now a Prometheus reserved word

## [7.21.1] - 2024-07-15

### Changes

- Fixed bug preventing the application to start when the snapshot feature was not active

## [7.21.0] - 2024-07-12

### Changes

- Update parent from 23.22.1 to 24.4.0
- Dropped some indices after an usage analysis (JEAP-4468)
 
## [7.20.0] - 2024-07-10

### Changes

- Added the capability to view process snapshots in the UI if a process instance has been deleted

## [7.19.0] - 2024-07-09

### Changes

- Added the option to configure the creation of process snapshots
- Added a process archive REST interface implementation for process snapshots
- Added the publishing of notification events for created snapshots
- Added housekeeping for snapshots
- Upgraded to jeap parent version 23.22.1

## [7.18.0] - 2024-05-06

### Changes

- Update frontend to Oblique version 11.1.0 and Angular version 17.3.6, including the toolchain
- Update to typescript 5

## [7.17.0] - 2024-04-26

### Changes

- Update parent from 23.12.1 to 23.16.0
- Move avro messages definitions to jeap-message-type-registry and get generated classes as dependency

## [7.16.1] - 2024-03-28

### Changes

- Added button to open logs related to given trace id for messages in UI
- Update parent from 23.12.0 to 23.12.1

## [7.16.0] - 2024-03-28

### Changes

- Update parent from 23.11.1 to 23.12.0

### Bugfix

- Task View will be activated when clicking on Process ID Link in Process Relations

## [7.15.0] - 2024-03-26

### Changed

- Trace Id of messages is now persisted and shown in PCS UI

## [7.14.1] - 2024-03-25

### Changed

- Removed unique constraint on process_instance_id in table process_instance_process_relations so that the same process
  can have multiple relations

## [7.14.0] - 2024-03-14

### Changed

- Update parent from 23.10.0 to 23.10.4

## [7.13.0] - 2024-03-05

### Changed

- Upgraded to jeap parent 23.10.0 (Spring Boot 3.2.3)

## [7.12.1] - 2024-02-05

### Changed

- Improve direct frontend route navigation redirection

## [7.12.0] - 2024-02-05

### Changed

- Update parent from 22.5.0 to 23.0.0

## [7.11.1] - 2024-01-31

### Fixed

- Fix mapping of Role in MessageData and MessageDTO

## [7.11.0] - 2024-01-29

### Changed

- Add new feature "joinType" to RelationPatterns

## [7.10.0] - 2024-01-25

### Changed

- Update parent from 22.2.3 to 22.5.0

## [7.9.0] - 2024-01-23

### Changed

- Update parent from 22.1.0 to 22.2.3

## [7.8.0] - 2024-01-16

### Changed

- Update parent from 22.0.0 to 22.1.0

## [7.7.2] - 2024-01-16

### Bugfix

- Navigating in UI works again

## [7.7.1] - 2024-01-12

### Changed

- Messages in UI show now most recent first

## [7.7.0] - 2024-01-11

### Changed

- Trigger the creation/completion of tasks after the creation of a process instance from a domain event

## [7.6.0] - 2024-01-09

### Changed

- Update parent from 21.3.1 to 22.0.0

## [7.5.0] - 2024-01-03

- upgrade to jeap-parent 21.3.1
- remove bootstrap configuration

## [7.4.0] - 2023-12-21

- upgrade to jeap-parent 21.2.2
- add multi cluster support for messages defined in process template

## [7.3.0] - 2023-12-14

- upgrade to jeap-parent 21.2.0 (spring boot 3.2)

## [7.2.1] - 2023-12-12

### Fixed

Set correct frontend origin based on application URL configuration

## [7.2.0] - 2023-12-07

- added support for conditional task instantiation for observed and dynamic tasks

## [7.1.0] - 2023-11-27

- added process relations

## [7.0.1] - 2023-11-28

- fix housekeeping lock-up issues

## [7.0.0] - 2023-11-21

- ugprade to jeap-parent 21.0.0 with multicluster support in jeap-messaging

## [6.3.0] - 2023-10-23

- added the activity view of tasks
- added italian translations

## [6.2.0] - 2023-10-02

- support for internationalization of process templates
- internationalization of all texts in UI
- update jeap-spring-boot-parent from 20.5.0 to 20.6.1

## [6.1.0] - 2023-09-18

- add support for observing process-related commands
- rename event to message in API, templates and internal models (backwards compatible)

## [6.0.3] - 2023-08-25

- update flyway scripts for h2 to work lowercase

## [6.0.2] - 2023-08-25

- Set spring.jpa.properties.hibernate.timezone.default_storage=NORMALIZE

## [6.0.1] - 2023-08-25

- Update parent from 20.0.0 to 20.0.2
- Remove hardcoded public prefix in flyway script for h2 (only for unittests)

## [6.0.0] - 2023-08-16

- Migration Spring Boot 3

## [5.33.0] - 2023-08-09

### Changed

- Update parent from 19.16.1 to 19.17.0

## [5.32.0] - 2023-08-09
- Changed the RelationListener plugin interface to accept a collection of relations instead of just one relation. This is a
  breaking change in the plugin API and you will have to upgrade the Transparenza process context relation listener dependency
  in your PCS to a matching version, i.e. >= 2.5.0.

## [5.31.0] - 2023-08-08

### Changed

- Update parent from 19.15.1 to 19.16.1

## [5.30.1] - 2023-07-18

- Update to parent 19.15.1
- Re-using embedded kafka / spring context to speed up integration tests

## [5.30.0] - 2023-07-12

- Fixed missing contract exception thrown for ProcessContextOutdatedEvent and ProcessContextStateChangedEvent.
- Fixed late correlation would correlate events from an unrelated process under specific circumstances.
- Added a configuration option and a plugin interface to allow the process instantiation by an event to depend on a condition.

## [5.29.1] - 2023-07-07

### Fixed

- works now with PAMS

### Changed

- Changed the Icons in UI from because Oblique does not support Fontawesome anymore


## [5.29.0] - 2023-06-28

### Changed

- updated UI to Oblique 10 and Angular 15
- added Cypress Component Tests

## [5.28.0] - 2023-06-22

### Changed

- updated to jeap-spring-boot-parent 19.13.0 (refactoring of avro serialization and deserialization in jeap-messaging)
- removed deprecated support for internal messages as strings

## [5.27.0] - 2023-06-13

### Changed

- Improved accuracy of metrics, added new metrics for state change events

## [5.26.0] - 2023-05-30

### Changed

- Update parent from 19.10.1 to 19.12.1

## [5.25.0] - 2023-05-24

### Changed

- Replace UUID v4 with UUID v7 (time-based UUID bit layout based on the Unix Epoch timestamp)

## [5.24.1] - 2023-05-08

### Fixed

- Fixed an exception being thrown when calling getReferences() on messages with no references, which since 
jeap-messaging 4.8.0 are optional 

## [5.24.0] - 2023-05-05

### Changed

- Introduce new time serie in ProcessEventService
- Replace @JeapTimed with @Timed

## [5.23.0] - 2023-04-06

### Changed

- ProcessDetail UI:
  - ID not in the title anymore
  - Task: Added plannedAt and completedAt. Removed some technical details.
  - Process Data, Events, Milestone and Relations are not collapsed by default anymore

## [5.22.0] - 2023-04-21

### Changed

- Update parent from 19.10.0 to 19.10.1

## [5.21.0] - 2023-04-18

### Added

- Introduce further time series for system performance evaluation

## [5.20.0] - 2023-03-31

### Changed

- Update parent from 19.2.0 to 19.8.0

### Added

- Fields plannedAt and completedAt to entity TaskInstance

## [5.19.1] - 2023-03-29

### Changed

- Added missing unique contraint on 'events' table to prevent storing the same event (event_name, idempotence_id) twice
  when two copies of it are received at the same time by the PCS. Before upgrading to this PCS version the database
  must have been cleaned from data that violates this constraint (if such data happens to exist).
- Added additional indices to speed up the housekeeping.

### Fixed

- Fixed database schema problem.
- Don't fail when setting the transient ProcessTemplate on a process instance multiple times.

## [5.19.0] - 2023-03-21

**!!! Do not install this version, there is a problem in the database schema which is fixed by version 5.19.1 !!!**

### Added

- Added support for configuring custom process completions. This PCS version needs to add optional fields to the
  process_instance table. In order to minimize the time needed for the database migration make sure to set the
  configurable PCS housekeeping to clean-up as much old/completed processes as possible before the upgrade.

## [5.18.0] - 2023-03-14

### Added

- Support dynamic single-instance tasks
- Reflect optional tasks in template migration
- Observation tasks with DomainEvents
- Changed the internal handling of event data that is associated with a process instance (in order to improve performance).

## [5.17.0] - skipped

## [5.16.0] - skipped

## [5.15.0] - 2023-03-02

### Added

- Plan tasks with DomainEvents

## [5.14.0] - 2023-02-21

### Changed

- Update parent from 19.0.1 to 19.2.0

## [5.13.0] - 2023-02-14

### Changed

- Changed the task 'cardinality' configuration by adding support for the configuration values 'SINGLE_INSTANCE' and
  'MULTI_INSTANCE' while deprecating the previous values 'SINGLE' and 'DYNAMIC'. Added the task 'lifecycle' configuration
  option with supported configuration values of 'STATIC', 'DYNAMIC' and 'OBSERVED'. To upgrade an existing configuration,
  replace 'cardinality' = 'SINGLE' with 'lifecycle' = 'STATIC' and 'cardinality' = 'DYNAMIC' with 'lifecycle' = 'DYNAMIC'.
  Using the 'cardinality' option with the values 'SINGLE' or 'DYNAMIC' is deprecated, but at the moment still supported.

## [5.12.0] - 2023-02-10

- For performance reasons, remove Spring Data queries in favor of native queries for batch message deletion

## [5.11.0] - 2023-02-10

- Deprecate TaskPlanned / TaskCompleted events (will be replaced with task planning using domain events)

## [5.10.0] - 2023-02-08

### Changed

- Important: This release requires a non-zero-downtime deployment because of database schema changes. Otherwise, you risk
  losing process instance updates. A non-zero-downtime deployment should not be a problem as the process context
  service works asynchronously on events and a short downtime does not impact its functionality (except for the
  process instance inspection UI, of course). To keep the number of process instances affected by the database
  migration on deployment small, your PCS instance should already have been configured to remove completed
  process instances rather sooner than later (see PCS housekeeping configuration).
- Improved performance when adding process data or relations and when there is a lag on process context outdated events.
- Relaxed unique condition of task instances to process instance id, origin task id and task type.

## [5.9.0] - 2023-02-01

### Added

- Add view `process-instance-by-id` for deep linking.


## [5.8.2] - 2023-01-13

### Fixed

- Upgraded to jeap parent 18.9.1.
- Fixed incompatibility with jeap security 11.5.0.

## [5.8.1] - 2022-12-12

### Fixed

- Fix incorrect task type check when completing a task by name

## [5.8.0] - 2022-12-12

### Changed

- Update parent from 18.5.0 to 18.6.2
- RestApiSecurityConfig: use new version of JeapJwtDecoderFactory
- ProcessContextContractsValidator use the new validator of jeap-messaging

## [5.7.0] - 2022-11-28

### Changed

- Update parent from 18.2.0 to 18.5.0

## [5.6.0] - 2022-10-31

### Changed

- Update parent from 18.0.0 to 18.2.0

## [5.5.1] - 2022-10-10

### Changed
- Restoring same SecurityFilterChain bean ordering in FrontendWebSecurityConfig as before on the deprecated WebSecurityConfigurerAdapter (by default 100).

## [5.5.0] - 2022-10-06

### Changed
- Updated parent from 17.3.0 to 18.0.0 (spring boot 2.7)

## [5.4.0] - 2022-09-21

### Changed

- Update parent from 17.2.2 to 17.3.0

## [5.3.1] - 2022-09-20

### Fixed

- Fixed the search query by process data to return only distinct processes

## [5.3.0] - 2022-09-13

### Changed

- Update parent from 17.0.0 to 17.2.2
- Remove component scan of jeap messaging and import kafka configurations separately in integration tests

## [5.2.0] - 12.08.2022

### Changed

- the property `triggersProcessInstantiation` is now a boolean
- updated to jeap-parent 17.0.0

## [5.1.2] - 30.05.2022

### Changed

- Set unique consumer group ID for domain event consumers per topic/eventname pair

## [5.1.1] - 30.05.2022

### Fixed

- Fixed duplicated process data exception in ProcessInstance when adding the same data with another role

## [5.1.0] - 19.05.2022

### Added

- Search with Process Data in UI

## [5.0.2] - 16.05.2022

### Changed

- Updated to jeap-parent 15.11.1 with jeap-messaging 3.7.1, no functional changes

## [5.0.1] - 13.05.2022

### Fixed

- MessageProcessingFailedEvent is no longer produced if a domain event with a (yet) unknown process ID is received. This
  is an expected situation when using async process instantiation and should not lead to an error.

## [5.0.0] - 09.05.2022

### Added

- Process instances can be created with the new command CreateProcessInstanceCommand. For this, a new topic must be
  created and defined with the variable `jeap.processcontext.kafka.topic.create-process-instance`.
- Process instances can be created with a domain event if the property `triggersProcessInstantiation` is set to true

### Changed

- External references from process instance deleted. Process data should be used instead. The rest interface still uses external references for the instantiation but this method of instantiation is deprecated. In the future, projects should preferably use either the instantiation with domain event or with the new command.
- Upgraded to jeap-spring-boot-parent 15.10.0


## [4.1.0] - 28.04.2022

### Changed

- Added systemId to Relation

## [4.0.2] - 14.04.2022

### Fixed

- Fixed security requirement declaration in OpenAPI config.

## [4.0.1] - 21.03.2022

### Fixed

- Fixed the paging problem in the housekeeping service

## [4.0.0] - 08.03.2022

### Changed

- The process context service no longer sets the 'javax.net.ssl.trustStore' property to the 'truststore.jks' resource on start-up.
  Therefore, you now will have to define the truststore for SSL connections yourself in your process context service instance. For
  microservices in Cloudfoundry you can do this e.g. in the Cloudfoundry manifest file.
- Upgraded to jeap-spring-boot-parent 15.5.1.

## [3.17.0] - 08.03.2022

### Added

* Create new task instances or mark old task instances as deleted in case of template migration
* Create new milestones instances or mark old milestones instances as deleted in case of template migration

## [3.16.0] - 04.02.2022

### Added

* Store template hash for migration detection

## [3.15.1] - 27.01.2022

### Fixed

* Removed initial non-null constraint on failed column when altering table

## [3.15.0] - 27.01.2022

### Changed

* Mark process updates as failed if unable to apply and continue with the next update

## [3.14.1] - 21.01.2022

### Added

* Displays the POM-Version from jEAP Process Context Service in UI

## [3.14.0] - 19.01.2022

### Changed

* Using avro domain events for internal communication for better integration with the jEAP error handling.
  This change is backwards-compatible.

## [3.13.0] - 14.01.2022

### Added

* Added housekeeping scheduling service to delete old data.

## [3.12.2] - 23.12.2021

### Changed

* update to jeap-spring-boot-parent 15.2.0 (spring boot 2.6.2)

## [3.12.1] - 14.12.2021

### Changed

* Correct error message when an unknown process origin ID is encountered in events (instead of NPE)

## [3.12.0] - 26.10.2021

### Added

* Added support for correlating a domain event by process data. Please see the jEAP blog post from October 26th 2021 for
  additional details.

## [3.11.0] - 15.09.2021

### Changed

* Refactored event storage: incoming events and their extracted data are now stored separately from process instances.
  This will allow late correlation of incoming events to process instances. When upgrading a process context service to
  this version make sure only one instance of the service is running in order to allow a clean migration of the
  persisted data to the refactored database schema. The flyway data migration might take quite a while on startup of the
  service if there are lots of events to migrate.
* Please note that the default flyway locations have changed, ```db/migration/postgres``` has been added:
  ```spring.flyway.locations=classpath:db/migration/common, classpath:db/migration/postgres, classpath:db/migration/postgrescluster```

## [3.10.0] - 21.07.2021

### Added

* UI: Added External References
* UI: Added Process Data
* UI: Added Event Data for each Event

## [3.9.0] - 09.07.2021

### Changed

* Upgraded to jeap-spring-boot-parent 14.2.0 (optimized kafka default configuration)

## [3.8.1] - 23.06.2021

### Changed

* Upgraded to jeap-spring-boot-parent 14.0.4 (spring boot 2.5.1)
* Renamed profile 'local' to 'local-pcs' to prevent it from being activated by the teams in their local PCS instances.

## [3.8.0] - 08.06.2021

### Added

* UI: Search with ProcessOriginId
* UI: Search with ExternalReference

### [3.7.1] - 04.06.2021

- Update jEAP parent to 14.0.3

### [3.7.0] - 01.06.2021

- Update jEAP parent to 14.0.0, including spring boot 2.5.0

### [3.6.3] - 12.05.2021

### Fixed

- Fix domain event acknowledge handling

## [3.6.2] - 10.05.2021

### Changed

- Remove unnecessary log statement

## [3.6.1] - 10.05.2021

### Changed

- Include contracts from message type registry when checking for publisher contracts

## [3.6.0] - 05.05.2021

### Changed

- Add 'relationshipPatterns' in ProcessTemplate

## [3.5.0] - 20.04.2021

### Changed

- Add ReferencesExtractor Interface to extract references from incoming events
- Add ProcessData in ProcessInstance and optional Role in EventData

## [3.4.1] - 08.03.2021

### Changed

- Updated jeap-spring-boot-parent to 13.6.7
- Updated QDAuth-Service to v1.0.19

## [3.4.0] - 24.02.2021

### Changed

- Added the option to correlate one domain event to more than one process instances.
- Added the option to correlate a domain event to a process instance by querying the external references of the existing
  process instances.

## [3.3.0] - 22.02.2021

### Changed

- Updated jeap-spring-boot-parent to 13.6.2 (REST tracing improvements, jeap-messaging contract logging improvements)

## [3.2.0] - 09.02.2021

### Changed

- Added the option to complete a task with a single instance by its task type name.

## [3.1.0] - 02.02.2021

### Changed

- Add PayloadExtractor Interface to extract data from incoming events

## [3.0.1] - 29.01.2021

### Changed

- Use QDAuth-Service v1.0.12 for jeap-process-context-ui

## [3.0.0] - 28.01.2021

### Changed

- Migrate from domainevent to jeap-messaging

## [2.0.2] - 15.01.2021

### Changed

- jeap-process-context-ui: Updated to Angular 10, Oblique 5 and integrated the QuadrelAuthService

## [2.0.1] - 17.12.2020

### Changed

- Changed Semantic Roles Resource from process-instance to processinstance

## [2.0.0] - 15.12.2020

### Changed

- Added Semantic Roles

## [1.3.6] - 2020-11-26

### Changed

- Updated jeap parent to 12.0.0 including javadoc and sources

## [1.3.5] - 2020-09-03

### Changed

- Updated jeap parent to 11.0.0

## [1.3.4] - 2020-09-03

### Changed

- Updated jeap parent to 9.0.0-76

## [1.3.3] - 2020-08-13

### Added

- Persist task planned event in process context

### Changed

- TasksCompletedMilestoneCondition accepts multiple task names

## [1.3.2] - 2020-08-11

### Added

- Improved UI design

## [1.3.1] - 2020-08-11

### Added

- Show milestones and events in UI

## [1.3.0] - 2020-07-31

### Added

- Listen to domain events, correlate domain events to process / tasks
- Add domain events to process context
- Add task completion condition based on domain events

## [1.2.0] - 2020-07-27

### Added

- Added milestones & milestone conditions
- Produce MilestoneReached events

## [1.1.0] - 2020-06-16

### Added

- Added Process-UI
- Comsume and Process TaskPlanned and TaskCompleted-Events
- Produce ProcessCreated and ProcessCompleted Events
- Configurable Role and Topic-Names
- Refactoring

## [1.0.1] - 2020-06-23

### Fixed

- Set default datasource URL to make application start on CF

## [1.0.0] - 2020-06-22

### Added

- Initial Skeleton

### Changed

Nothing

### Removed

Nothing
