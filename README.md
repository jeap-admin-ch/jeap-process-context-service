# jEAP Process Context Service
Service to check and visualize process context

## Installing / Getting started

Normally you will not use this project directly, but instead set up your own process context service depending on this
common library. Check the documentation for details.

## Changes

Change log is available at [CHANGELOG.md](./CHANGELOG.md)

## Local tests

- Open jme-process-context-example
    - cd docker
    - docker-compose-up -d
    - Start: jme-process-context-auth-scs
    - Start: jme-process-context-app-service
    - Start: jme-process-context-scs / UI via 'npm start' (Profiles: local,local-npm-ui)
  
- Open jme-process-context-service
    - Start: jeap-process-context-ui (npm run start) 
    - Access the frontend at http://localhost:4200/
    - Create Testevents with http://localhost:8082/jme-process-context-app-service/swagger-ui/index.html#/

## Local Cypress Component Tests

- Open terminal and cd jeap-process-context-ui
- Testing in Browser with UI: npx cypress open --port 7000
- Testing in Console: npm run cypress:run

## Note

This repository is part of the open source distribution of jEAP. See [github.com/jeap-admin-ch/jeap](https://github.com/jeap-admin-ch/jeap)
for more information.

## License

This repository is Open Source Software licensed under the [Apache License 2.0](./LICENSE).
