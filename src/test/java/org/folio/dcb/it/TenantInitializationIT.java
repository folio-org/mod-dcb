package org.folio.dcb.it;

import static com.github.tomakehurst.wiremock.client.WireMock.requestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.folio.dcb.support.wiremock.WiremockContainerExtension.getWireMockClient;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import org.folio.dcb.it.base.BaseIntegrationTest;
import org.folio.dcb.support.types.IntegrationTest;
import org.folio.dcb.support.wiremock.WireMockStub;
import org.folio.dcb.utils.DCBConstants;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

@IntegrationTest
class TenantInitializationIT extends BaseIntegrationTest {

  @Test
  @WireMockStub(value = {
    // stubs for entities that being queries several times (GET and POST requests)
    "/stubs/mod-inventory-storage/instance-types/200-get-by-query(dcb empty).json",
    "/stubs/mod-inventory/instances/404-get-by-id(dcb).json",
    "/stubs/mod-inventory-storage/location-units/institutions/404-get-by-id(dcb).json",
    "/stubs/mod-inventory-storage/location-units/campuses/200-get-by-query(dcb empty).json",
    "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(dcb empty).json",
    "/stubs/mod-inventory-storage/service-points/200-get-by-query(dcb empty).json",
    "/stubs/mod-inventory-storage/locations/200-get-by-query(dcb empty).json",
    "/stubs/mod-inventory-storage/holdings-sources/200-get-by-query(dcb empty).json",
    "/stubs/mod-inventory-storage/holdings-storage/404-get-by-id(dcb).json",
    "/stubs/mod-circulation-storage/cancellation-reason-storage/404-get-by-id(dcb).json",
    "/stubs/mod-inventory-storage/loan-types/200-get-by-query(dcb empty).json",
    "/stubs/mod-calendar/calendars/200-get-all(empty).json",

    // stubs to for POST request for DCB locations and location units
    "/stubs/mod-inventory-storage/location-units/institutions/201-post(dcb).json",
    "/stubs/mod-inventory-storage/location-units/campuses/201-post(dcb).json",
    "/stubs/mod-inventory-storage/location-units/libraries/201-post(dcb).json",
    "/stubs/mod-inventory-storage/service-points/201-post(dcb).json",
    "/stubs/mod-inventory-storage/locations/201-post(dcb).json",
    "/stubs/mod-inventory-storage/instance-types/201-post(dcb).json",
    "/stubs/mod-inventory/instances/201-post(dcb).json",
    "/stubs/mod-inventory-storage/holdings-sources/201-post(dcb).json",
    "/stubs/mod-inventory-storage/holdings-storage/201-post(dcb).json",
    "/stubs/mod-circulation-storage/cancellation-reason-storage/201-post(dcb).json",
    "/stubs/mod-inventory-storage/loan-types/201-post(dcb).json",
    "/stubs/mod-calendar/calendars/201-post(dcb).json",
  })
  void initializeTenant_positive_noDcbEntitiesPresent() {
    enableTenant();

    assertThatApiIsCalledOnce(POST, "/location-units/institutions");
    assertThatApiIsCalledOnce(POST, "/location-units/campuses");
    assertThatApiIsCalledOnce(POST, "/location-units/libraries");
    assertThatApiIsCalledOnce(POST, "/locations");
    assertThatApiIsCalledOnce(POST, "/service-points");
    assertThatApiIsCalledOnce(POST, "/instance-types");
    assertThatApiIsCalledOnce(POST, "/inventory/instances");
    assertThatApiIsCalledOnce(POST, "/holdings-sources");
    assertThatApiIsCalledOnce(POST, "/holdings-storage/holdings");
    assertThatApiIsCalledOnce(POST, "/loan-types");
    assertThatApiIsCalledOnce(POST, "/cancellation-reason-storage/cancellation-reasons");
    assertThatApiIsCalledOnce(POST, "/calendar/calendars");

    purgeTenant();
  }

  @Test
  @WireMockStub(value = {
    "/stubs/mod-inventory-storage/instance-types/200-get-by-query(dcb).json",
    "/stubs/mod-inventory/instances/200-get-by-id(dcb).json",
    "/stubs/mod-inventory-storage/location-units/institutions/200-get-by-id(dcb).json",
    "/stubs/mod-inventory-storage/location-units/campuses/200-get-by-query(dcb).json",
    "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(dcb).json",
    "/stubs/mod-inventory-storage/service-points/200-get-by-query(dcb).json",
    "/stubs/mod-inventory-storage/locations/200-get-by-query(dcb).json",
    "/stubs/mod-inventory-storage/holdings-storage/200-get-by-id(dcb).json",
    "/stubs/mod-circulation-storage/cancellation-reason-storage/200-get-by-id(dcb).json",
    "/stubs/mod-inventory-storage/loan-types/200-get-by-query(dcb).json",
    "/stubs/mod-calendar/calendars/200-get-all.json"
  })
  void initializeTenant_positive_dcbEntitiesExist() {
    enableTenant();

    assertThatApiIsCalledOnce(GET, "/locations");
    assertThatApiIsCalledOnce(GET, "/holdings-storage/holdings/" + DCBConstants.HOLDING_ID);
    assertThatApiIsCalledOnce(GET,
      "/cancellation-reason-storage/cancellation-reasons/" + DCBConstants.CANCELLATION_REASON_ID);
    assertThatApiIsCalledOnce(GET, "/loan-types");
    assertThatApiIsCalledOnce(GET, "/calendar/calendars");
    purgeTenant();
  }

  private static void assertThatApiIsCalledOnce(HttpMethod method, String urlPath) {
    var wiremock = getWireMockClient();
    wiremock.verifyThat(1, requestedFor(method.name(), urlPathEqualTo(urlPath)));
  }
}
