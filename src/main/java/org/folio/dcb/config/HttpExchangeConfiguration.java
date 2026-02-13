package org.folio.dcb.config;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.integration.calendar.CalendarClient;
import org.folio.dcb.integration.inventory.InstanceClient;
import org.folio.dcb.integration.users.GroupClient;
import org.folio.dcb.integration.users.UsersClient;
import org.folio.dcb.integration.invstorage.HoldingsStorageClient;
import org.folio.dcb.integration.invstorage.InstanceTypeClient;
import org.folio.dcb.integration.invstorage.InventoryItemStorageClient;
import org.folio.dcb.integration.invstorage.LocationsClient;
import org.folio.dcb.integration.invstorage.LocationUnitClient;
import org.folio.dcb.integration.invstorage.MaterialTypeClient;
import org.folio.dcb.integration.invstorage.ServicePointClient;
import org.folio.dcb.integration.invstorage.HoldingSourcesClient;
import org.folio.dcb.integration.circulation.CirculationClient;
import org.folio.dcb.integration.circstorage.CancellationReasonClient;
import org.folio.dcb.integration.circstorage.CirculationLoanPolicyStorageClient;
import org.folio.dcb.integration.circstorage.CirculationRequestClient;
import org.folio.dcb.integration.circitem.CirculationItemClient;
import org.folio.dcb.integration.circstorage.LoanTypeClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configuration class for HTTP exchange clients.
 * This class configures various REST clients for integration with FOLIO modules
 * such as calendar, inventory, users, circulation, etc., using Spring's HttpServiceProxyFactory.
 */
@Log4j2
@Configuration
public class HttpExchangeConfiguration {

  /**
   * Creates a JsonMapper bean configured for HTTP exchanges.
   * The mapper includes non-null values, disables failure on unknown properties,
   * disables failure on null for primitives, and enables accepting single value as array.
   *
   * @return the configured JsonMapper instance
   */
  @Bean
  public JsonMapper exchangeJsonMapper() {
    return JsonMapper.builder()
      .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(Include.NON_NULL))
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
      .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
      .build();
  }

  /**
   * Creates a primary RestClient.Builder bean for DCB.
   * Configures the builder with a custom JacksonJsonHttpMessageConverter using the exchange JsonMapper.
   *
   * @param restClientBuilder the base RestClient.Builder
   * @param mapper the JsonMapper to use for JSON conversion
   * @return the configured RestClient.Builder instance
   */
  @Bean
  @Primary
  public RestClient.Builder dcbRestClientBuilder(RestClient.Builder restClientBuilder,
    @Qualifier("exchangeJsonMapper") JsonMapper mapper) {
    restClientBuilder.configureMessageConverters(configurer ->
      configurer.addCustomConverter(new JacksonJsonHttpMessageConverter(mapper)));
    return restClientBuilder;
  }

  /**
   * Creates a {@link CalendarClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link CalendarClient} instance
   */
  @Bean
  public CalendarClient calendarClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CalendarClient.class);
  }

  /**
   * Creates an {@link InstanceClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link InstanceClient} instance
   */
  @Bean
  public InstanceClient instanceClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InstanceClient.class);
  }

  /**
   * Creates a {@link GroupClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link GroupClient} instance
   */
  @Bean
  public GroupClient groupClient(HttpServiceProxyFactory factory) {
    return factory.createClient(GroupClient.class);
  }

  /**
   * Creates a {@link UsersClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link UsersClient} instance
   */
  @Bean
  public UsersClient usersClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UsersClient.class);
  }

  /**
   * Creates a {@link HoldingsStorageClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link HoldingsStorageClient} instance
   */
  @Bean
  public HoldingsStorageClient holdingsStorageClient(HttpServiceProxyFactory factory) {
    return factory.createClient(HoldingsStorageClient.class);
  }

  /**
   * Creates an {@link InstanceTypeClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the InstanceTypeClient instance
   */
  @Bean
  public InstanceTypeClient instanceTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InstanceTypeClient.class);
  }

  /**
   * Creates an {@link InventoryItemStorageClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the InventoryItemStorageClient instance
   */
  @Bean
  public InventoryItemStorageClient inventoryItemStorageClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InventoryItemStorageClient.class);
  }

  /**
   * Creates a {@link LocationsClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the LocationsClient instance
   */
  @Bean
  public LocationsClient locationsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LocationsClient.class);
  }

  /**
   * Creates a {@link LocationUnitClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link LocationUnitClient} instance
   */
  @Bean
  public LocationUnitClient locationUnitClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LocationUnitClient.class);
  }

  /**
   * Creates a {@link MaterialTypeClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link MaterialTypeClient} instance
   */
  @Bean
  public MaterialTypeClient materialTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(MaterialTypeClient.class);
  }

  /**
   * Creates a {@link ServicePointClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link ServicePointClient} instance
   */
  @Bean
  public ServicePointClient servicePointClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ServicePointClient.class);
  }

  /**
   * Creates a {@link HoldingSourcesClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} beam
   * @return the {@link HoldingSourcesClient} instance
   */
  @Bean
  public HoldingSourcesClient holdingSourcesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(HoldingSourcesClient.class);
  }

  /**
   * Creates a {@link CirculationClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link CirculationClient} instance
   */
  @Bean
  public CirculationClient circulationClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CirculationClient.class);
  }

  /**
   * Creates a {@link CancellationReasonClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link CancellationReasonClient} instance
   */
  @Bean
  public CancellationReasonClient cancellationReasonClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CancellationReasonClient.class);
  }

  /**
   * Creates a {@link CirculationLoanPolicyStorageClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link CirculationLoanPolicyStorageClient} instance
   */
  @Bean
  public CirculationLoanPolicyStorageClient circulationLoanPolicyStorageClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CirculationLoanPolicyStorageClient.class);
  }

  /**
   * Creates a {@link CirculationRequestClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link CirculationRequestClient} instance
   */
  @Bean
  public CirculationRequestClient circulationRequestClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CirculationRequestClient.class);
  }

  /**
   * Creates a {@link CirculationItemClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link CirculationItemClient} instance
   */
  @Bean
  public CirculationItemClient circulationItemClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CirculationItemClient.class);
  }

  /**
   * Creates a {@link LoanTypeClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link LoanTypeClient} instance
   */
  @Bean
  public LoanTypeClient loanTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LoanTypeClient.class);
  }
}
