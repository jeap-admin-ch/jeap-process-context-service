# jEAP Process Context Service
Service to check and visualize process context data. 
This project provides a common library for the process context service, which can be used to set up a process context service instance. It also provides a UI to visualize the process context data and an example application that uses the process context service.

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

### PAMS / ePortal

The UI header contains the ePortal service navigation of Oblique, which is backed by PAMS
(`https://pams-api.eportal<environment>.admin.ch`). It is configured with the following properties under
`jeap.processcontext.frontend`:

| Property           | Description                                                                                                                 | Default |
|--------------------|-----------------------------------------------------------------------------------------------------------------------------|---------|
| `pams-enabled`     | Whether the application is integrated with PAMS/ePortal.                                                                     | `true`  |
| `pams-environment` | ePortal environment of the service navigation: `DEV`, `TEST`, `REF`, `ABN` or `PROD`. Not required if PAMS is disabled.      | -       |
| `mock-pams`        | Treat the PAMS session as always active instead of reading it from the service navigation. Implied by `pams-enabled: false`. | `false` |

Set `pams-enabled: false` for deployments without PAMS:

```yaml
jeap:
  processcontext:
    frontend:
      pams-enabled: false
```

The UI then does not contact the ePortal backend at all - no service navigation requests, no ePortal session
timeout handling - and authentication is based solely on OAuth2/OIDC. The header controls served by PAMS
(login/logout, profile, messages, applications) would be non-functional and are hidden; the language selection
remains available.

Note that `pams-environment` must match the environment of the identity provider the UI authenticates
against. Pointing the service navigation at a different environment than the authentication leads to an
inconsistent login state in the header and to logout and timeout redirects into the wrong ePortal.

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
