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
  - [Service point hold shelf period expiration](#service-point-hold-shelf-period-expiration)
  - [Other documentation](#other-documentation)

## Introduction

APIs for managing DCB (direct consortial borrowing) transactions in folio.

## API information

dcb API provides the following URLs:

| Method | URL                                                     | Permissions                           | Description                                                                         |
|--------|---------------------------------------------------------|---------------------------------------|-------------------------------------------------------------------------------------|
| GET    | /transactions/{dcbTransactionId}/status                      | dcb.transactions.get      | Gets status of transaction based on transactionId                                   |
| POST   | /transactions/{dcbTransactionId}                                             | dcb.transactions.post        | create new transaction                                                              |
| PUT    | /transactions/{dcbTransactionId}/status                            | dcb.transactions.put         | Update the status of the transaction and it will trigger automatic action if needed |
| GET    | /transactions/{dcbTransactionId}/status                            | dcb.transactions.collection.get         | get list of transaction updated between a given query range |

## Installing and deployment

### Compiling

Compile with
```shell
mvn clean install
```

### Running it

Run locally on listening port 8081 (default listening port):

Using Docker to run the local stand-alone instance:

```shell
DB_HOST=localhost DB_PORT=5432 DB_DATABASE=okapi_modules DB_USERNAME=folio_admin DB_PASSWORD=folio_admin \
   java -Dserver.port=8081 -jar target/mod-dcb-*.jar
```

### Docker

Build the docker container with:

```shell
docker build -t dev.folio/mod-dcb .
```

### Module Descriptor

See the built `target/ModuleDescriptor.json` for the interfaces that this module
requires and provides, the permissions, and the additional module metadata.

### Environment variables

| Name                                      |    Default value    | Description                                                                                                                                                         |
|:------------------------------------------|:-------------------:|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| DB_HOST                                   |      postgres       | Postgres hostname                                                                                                                                                   |
| DB_PORT                                   |        5432         | Postgres port                                                                                                                                                       |
| DB_USERNAME                               |     folio_admin     | Postgres username                                                                                                                                                   |
| DB_PASSWORD                               |          -          | Postgres username password                                                                                                                                          |
| DB_DATABASE                               |    okapi_modules    | Postgres database name                                                                                                                                              |
| KAFKA_HOST                                |        kafka        | Kafka broker hostname                                                                                                                                               |
| KAFKA_PORT                                |        9092         | Kafka broker port                                                                                                                                                   |
| KAFKA_SECURITY_PROTOCOL                   |      PLAINTEXT      | Kafka security protocol used to communicate with brokers (SSL or PLAINTEXT)                                                                                         |
| KAFKA_SSL_KEYSTORE_LOCATION               |          -          | The location of the Kafka key store file. This is optional for client and can be used for two-way authentication for client.                                        |
| KAFKA_SSL_KEYSTORE_PASSWORD               |          -          | The store password for the Kafka key store file. This is optional for client and only needed if 'ssl.keystore.location' is configured.                              |
| KAFKA_SSL_TRUSTSTORE_LOCATION             |          -          | The location of the Kafka trust store file.                                                                                                                         |
| KAFKA_SSL_TRUSTSTORE_PASSWORD             |          -          | The password for the Kafka trust store file. If a password is not set, trust store file configured will still be used, but integrity checking is disabled.          |
| ENV                                       |        folio        | Environment. Logical name of the deployment, must be set if Kafka/Elasticsearch are shared for environments, `a-z (any case)`, `0-9`, `-`, `_` symbols only allowed |
| SYSTEM\_USER\_NAME                        |   dcb-system-user   | Username of the system user                                                                                                                                         |
| SYSTEM\_USER\_PASSWORD                    |          -          | Password of the system user                                                                                                                                         |
| SYSTEM\_USER\_ENABLED                     |        true         | Defines if system user must be created at service tenant initialization or used for egress service requests                                                         |
| ACTUATOR\_EXPOSURE                        | health,info,loggers | Back End Module Health Check Protocol                                                                                                                               |
| FLEXIBLE_CIRCULATION_RULES_ENABLED        |        true         | If enabled, new virtual items will be attempted to assign with a shadow location based on `item.lendingLibraryCode` or `item.locationCode`                          |
| DCB_ENTITIES_RUNTIME_VERIFICATION_ENABLED |        true         | If enabled, all DCB-controlled entities (virtual service point, instance, holding, etc.) will be verified before using them                                         |

## Additional information

### System user configuration
The module uses system user to communicate with other modules.
For production deployments you MUST specify the password for this system user via env variable:
`SYSTEM_USER_PASSWORD=<password>`.

### DCB-HUB Configuration
DCB hub secure-store parameters is used to connect to the dcb-hub module.
If dcb-hub FETCH_DCB_LOCATIONS_ENABLED is true, you must have the below:-
1. DCB_LOCATIONS_BASE_URL environment variable set to the BASE_URL of the dcb-hub.
2. `dcb-hub-credentials` key in secure-store with below JSON structure:
```json
{
  "client_id": "client-id-54321",
  "client_secret": "client_secret_54321",
  "username": "admin54321",
  "password": "admin54321",
  "keycloak_url": "KC-HOST/realms/master/protocol/openid-connect/token" // Full Absolute URL to the Keycloak token endpoint
}
```


### Issue tracker

See project [MODDCB](https://issues.folio.org/projects/MODDCB)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### ModuleDescriptor

See the built `target/ModuleDescriptor.json` for the interfaces that this module
requires and provides, the permissions, and the additional module metadata.

### API documentation

This module's [API documentation](https://dev.folio.org/reference/api/#mod-dcb).

### Code analysis

### Service Point Hold Shelf Period Expiration

When creating a **DCB** transaction with the roles **LENDER** or **BORROWING-PICKUP**, 
the creation of the **DCB** service point and its property **hold shelf expiration period** 
depends on the values stored in the `service_point_expiration_period` table in the database.

- If the table is empty, the **hold shelf expiration period** will be set to the default value of **10 Days**.
- If the table contains a value, the stored value will be used instead.

The **F.S.E. team** is responsible for updating the values in this table. 
To update the values, the following PL/pgSQL script can be executed:

```sql
DO
$$
DECLARE
    schema_name TEXT;
    new_duration INTEGER := 3;  -- Duration in weeks
    new_interval_id interval_id := 'Weeks';  -- Interval type
    raw_id UUID;
    sql_query TEXT;
BEGIN
    FOR schema_name IN
        SELECT schemaname
        FROM pg_tables
        WHERE tablename = 'service_point_expiration_period'
    LOOP
        -- Select a single ID into raw_id dynamically
        sql_query := format(
            'SELECT id FROM %I.service_point_expiration_period LIMIT 1',
            schema_name
        );
        EXECUTE sql_query INTO raw_id;

        -- If no record exists, insert one; otherwise, update the existing record
        IF raw_id IS NULL THEN
            sql_query := format(
                'INSERT INTO %I.service_point_expiration_period (id, duration, interval_id) 
                 VALUES (gen_random_uuid(), %L, %L)', 
                 schema_name, new_duration, new_interval_id
            );
        ELSE
            sql_query := format(
                'UPDATE %I.service_point_expiration_period 
                 SET duration = %L, interval_id = %L
                 WHERE id = %L', 
                 schema_name, new_duration, new_interval_id, raw_id
            );
        END IF;

        -- Execute the query
        EXECUTE sql_query;
    END LOOP;
END;
$$
LANGUAGE plpgsql;
```
**Updating Values in the Table**  
To update the values, simply modify the new_duration and new_interval_id variables in the DECLARE section 
of the script to reflect the new values.

**Expiration Period Handling**    
For Existing Service Points
When creating a new transaction with an existing DCB service point, the hold shelf expiration period 
will be checked.  
If the value in the transaction payload differs from the value stored 
in the database, it will be updated accordingly.

[SonarQube analysis](https://sonarcloud.io/project/overview?id=org.folio:mod-dcb).

## Other documentation

The built artifacts for this module are available.
See [configuration](https://dev.folio.org/download/artifacts) for repository access,
and the [Docker image](https://hub.docker.com/r/folioci/mod-dcb). Look at contribution guidelines [Contributing](https://dev.folio.org/guidelines/contributing).
