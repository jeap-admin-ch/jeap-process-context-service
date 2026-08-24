# jEAP Process Context Service

The jEAP Process Context Service (PCS) provides a process context for cross-service processes in an event
driven architecture, without having to control the execution of the processes with a central process engine
(choreography over orchestration). It consumes the domain events and commands of the participating services,
tracks the state of the process instances and their tasks, and visualizes them in a UI — so that a process can
be followed and analysed, and events can be published in reaction to changed process states or milestones.

This repository is published as a **library**: every business application creates and deploys its own PCS
instance depending on it, containing the process definitions specific to that application.

## Documentation

- [Getting Started](docs/getting-started.md) — set up a PCS instance for your business application
- [Architecture](docs/architecture.md) — goals, context, building blocks, domain model, cross-cutting concepts
- [Process Templates](docs/process-templates.md) — the complete process definition reference
- [Configuration](docs/configuration.md) — the property reference
- [User Interface](docs/user-interface.md) — views, roles, deep links and the log system link
- [Operations](docs/operations.md) — scaling, housekeeping, metrics and error handling
- [Maintenance Jobs](docs/maintenance.md) — relation reevaluation and process-data backfill
- [Template Migration](docs/template-migration.md) — what happens when a process template changes

## Changes

Change log is available at [CHANGELOG.md](./CHANGELOG.md)

## Prerequisites

Before you begin, ensure you have the following installed:

- Java 25 or higher
- npm (Node Package Manager) 22 or higher

**Note:** Use the provided Maven Wrapper (`mvnw` / `mvnw.cmd`) to build and run the project - no separate Maven installation required.

## Getting started

Normally you will not use this project directly, but instead set up your own process context service depending on this common library. 
If you want to test this library locally, you can use the example project [jme-process-context-example](https://github.com/jme-admin-ch/jme-process-context-example), which is available in a separate repository.
Check the documentation for details.

### Build the library

To build the library, run the following command in the root of the repository:

```bash
# Linux / macOS
./mvnw clean install
```

```bash
# Windows
mvnw.cmd clean install
```

The UI will also be built as part of this command, so there is no need to build it separately if you just want to use the library.

### Build the UI 

The UI can be built independently of the Spring Boot application, so it can be started separately. This is especially useful for development, as it allows to start the UI with hot reload and without the need to restart the Spring Boot application after every change.
To build the UI, run the following command in the root of the repository:

```bash
cd jme-process-context-ui
# Force is currently needed to resolve some dependency conflicts.
# This is a technical debt that will be resolved in the future by aligning the dependencies of the UI.
npm install --force
npm run build
```

### Start the UI locally

To start the UI locally, run the following command in the root of the repository:

```bash
cd jme-process-context-ui
npm run start
```

or via Angular CLI:

```bash
cd jme-process-context-ui
npx ng serve --open
```

These will start the UI on http://localhost:4200/. The UI will automatically reload after every change, so you can see the changes immediately in the browser. 
To run an example backend locally, you can use the example project [jme-process-context-example](https://github.com/jme-admin-ch/jme-process-context-example) or an own implementation of the process context service.
The UI will also automatically connect to the process context service running on http://localhost:8080/.

## Configuration

The properties of a PCS instance are documented in [Configuration](docs/configuration.md), including the
[PAMS and ePortal](docs/configuration.md#pams-and-eportal) settings of the UI header.

## Local Cypress Component Tests

### Testing in Browser with UI:

```bash
cd jeap-process-context-ui
npx cypress open --port 7000
```

### Testing in Console:

```bash
cd jeap-process-context-ui
npm run cypress:run
```

## Note

This repository is part of the open source distribution of jEAP. See [github.com/jeap-admin-ch/jeap](https://github.com/jeap-admin-ch/jeap)
for more information.

## License

This repository is Open Source Software licensed under the [Apache License 2.0](./LICENSE).
