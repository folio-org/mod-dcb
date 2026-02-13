# Automated HTTP Exchange Client Registration

## Overview

This project uses an automated approach to register all `@HttpExchange` annotated interfaces as Spring beans, eliminating the need for manual `@Bean` definitions.

## How It Works

### Before (Manual Registration)
Previously, you had to manually create a bean for each HTTP client:

```java
@Configuration
public class HttpExchangeConfiguration {

  @Bean
  public CalendarClient calendarClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CalendarClient.class);
  }

  @Bean
  public UsersClient usersClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UsersClient.class);
  }

  @Bean
  public CirculationClient circulationClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CirculationClient.class);
  }

  // ... many more bean definitions
}
```

### After (Automated Registration)
Now, the `HttpExchangeConfiguration` class automatically scans and registers all `@HttpExchange` interfaces:

```java
@Configuration
public class HttpExchangeConfiguration implements BeanFactoryPostProcessor {
  // Automatically scans org.folio.dcb.integration package
  // and registers all @HttpExchange interfaces as beans
}
```

## Creating New HTTP Clients

Simply create an interface annotated with `@HttpExchange` in the `org.folio.dcb.integration` package:

```java
package org.folio.dcb.integration.myservice;

import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.GetExchange;

@HttpExchange("my-service")
public interface MyServiceClient {

  @GetExchange("/data")
  MyData getData();
}
```

The client will be automatically registered as a Spring bean with the name `myServiceClient` (interface name with lowercase first letter).

## Using HTTP Clients

Inject the client like any other Spring bean:

```java
@Service
@RequiredArgsConstructor
public class MyService {

  private final CalendarClient calendarClient;
  private final UsersClient usersClient;

  public void doSomething() {
    Calendar calendar = calendarClient.createCalendar(...);
    User user = usersClient.createUser(...);
  }
}
```

## Currently Registered Clients

The following clients are automatically registered:
- `calendarClient` - CalendarClient
- `usersClient` - UsersClient
- `groupClient` - GroupClient
- `circulationClient` - CirculationClient
- `circulationItemClient` - CirculationItemClient
- `circulationRequestClient` - CirculationRequestClient
- `circulationLoanPolicyStorageClient` - CirculationLoanPolicyStorageClient
- `cancellationReasonClient` - CancellationReasonClient
- `instanceClient` - InstanceClient
- `inventoryItemStorageClient` - InventoryItemStorageClient
- `holdingsStorageClient` - HoldingsStorageClient
- `holdingSourcesClient` - HoldingSourcesClient
- `locationsClient` - LocationsClient
- `locationUnitClient` - LocationUnitClient
- `materialTypeClient` - MaterialTypeClient
- `servicePointClient` - ServicePointClient
- `instanceTypeClient` - InstanceTypeClient
- `loanTypeClient` - LoanTypeClient

## Technical Details

The implementation uses:
- **BeanFactoryPostProcessor** - Registers beans during Spring context initialization
- **ClassPathScanningCandidateComponentProvider** - Scans for classes with `@HttpExchange` annotation
- **GenericBeanDefinition** - Creates bean definitions dynamically
- **HttpServiceProxyFactory** - Creates the actual HTTP client proxies

## Benefits

1. ✅ **Less Boilerplate** - No need to write `@Bean` methods for each client
2. ✅ **Automatic Discovery** - New clients are automatically registered
3. ✅ **Consistent Naming** - Bean names follow a consistent convention
4. ✅ **Type Safety** - Full compile-time type checking
5. ✅ **Easy Maintenance** - Add new clients by just creating the interface

## Alternative Approaches

### Spring Framework 6.1+ (Future)
Spring Framework 6.1+ plans to introduce `@HttpExchangeScan`:

```java
@Configuration
@HttpExchangeScan(basePackages = "org.folio.dcb.integration")
public class HttpExchangeConfiguration {
  // Even simpler!
}
```

However, this is not yet available/stable in Spring Boot 4.0.2.

### Individual Bean Registration
If you need custom configuration for specific clients, you can still use manual registration:

```java
@Bean
public CustomClient customClient(HttpServiceProxyFactory factory) {
  // Custom configuration
  return factory.createClient(CustomClient.class);
}
```

The automated registration will not interfere with manually defined beans.

