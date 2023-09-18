# mod-dcb

Copyright (C) 2022-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Table of Contents

- [Introduction](#introduction)
- [API information](#api-information)
- [Installing and deployment](#installing-and-deployment)
  - [Compiling](#compiling)
  - [Running it](#running-it)
  - [Docker](#docker)
  - [Module descriptor](#module-descriptor)
  - [Environment variables](#environment-variables)
- [Additional information](#Additional-information)
  - [Issue tracker](#issue-tracker)
  - [API documentation](#api-documentation)
  - [Code analysis](#code-analysis)
  - [Other documentation](#other-documentation)

## Introduction

APIs for managing dcb transactions in folio.

## API information

dcb API provides the following URLs:

| Method | URL                                                     | Permissions                           | Description                                                                         |
|--------|---------------------------------------------------------|---------------------------------------|-------------------------------------------------------------------------------------|
| GET    | /transactions/{dcbTransactionId}/status                      | dcb.transactions.get      | Gets status of transaction based on transactionId                                   |
| POST   | /transactions/{dcbTransactionId}                                             | dcb.transactions.post        | create new transaction                                                              |
| PUT    | /transactions/{dcbTransactionId}/status                            | dcb.transactions.put         | Update the status of the transaction and it will trigger automatic action if needed |

## Installing and deployment

### Compiling

Compile with
```shell
mvn clean install
```

### Running it

To be added

### Docker

To be added

### Module Descriptor

See the built `target/ModuleDescriptor.json` for the interfaces that this module
requires and provides, the permissions, and the additional module metadata.

### Environment variables

| Name                   |  Default value  | Description                                                                                                                                            |
|:-----------------------|:---------------:|:-------------------------------------------------------------------------------------------------------------------------------------------------------|
| DB_HOST                |    postgres     | Postgres hostname                                                                                                                                      |
| DB_PORT                |      5432       | Postgres port                                                                                                                                          |
| DB_USERNAME            |   folio_admin   | Postgres username                                                                                                                                      |
| DB_PASSWORD            |        -        | Postgres username password                                                                                                                             |
| DB_DATABASE            |  okapi_modules  | Postgres database name                                                                                                                                 |
| ENV                    |      folio      | Environment                                                                                                                                            |
| KAFKA_HOST             |      kafka      | Kafka broker hostname                                                                                                                                  |
| KAFKA_PORT             |      9092       | Kafka broker port                                                                                                                                      |
| ENV                    |      folio      | Logical name of the deployment, must be set if Kafka/Elasticsearch are shared for environments, `a-z (any case)`, `0-9`, `-`, `_` symbols only allowed |
| SYSTEM\_USER\_NAME     | dcb-system-user | Username of the system user                                                                                                                            |
| SYSTEM\_USER\_PASSWORD |        -        | Password of the system user                                                                                                                            |

## Additional information

### Issue tracker

See project [MODDCB](https://issues.folio.org/projects/MODDCB)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### API documentation

This module's [API documentation](https://dev.folio.org/reference/api/#mod-dcb).

### Code analysis

[SonarQube analysis](https://sonarcloud.io/project/overview?id=org.folio:mod-dcb).

## Other documentation

The built artifacts for this module are available.
See [configuration](https://dev.folio.org/download/artifacts) for repository access,
and the [Docker image](https://hub.docker.com/r/folioci/mod-dcb). Look at contribution guidelines [Contributing](https://dev.folio.org/guidelines/contributing).
