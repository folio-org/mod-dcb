package org.folio.dcb.it;

import static com.github.tomakehurst.wiremock.client.WireMock.requestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.folio.dcb.support.wiremock.WiremockContainerExtension.getWireMockClient;

import org.folio.dcb.it.base.BaseIntegrationTest;
import org.folio.dcb.support.types.IntegrationTest;
import org.folio.dcb.support.wiremock.WireMockStub;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@TestPropertySource(properties = {
  "folio.system-user.enabled=true",
  "folio.system-user.password=dcb-system-user@password",
})
class TenantInitWithSystemUserIT extends BaseIntegrationTest {

  @Test
  @WireMockStub(value = {
    "/stubs/mod-users/users/200-get-user(system+empty).json",
    "/stubs/mod-users/users/201-post-user(system).json",
    "/stubs/mod-permissions/200-get-perms(system+empty).json",
    "/stubs/mod-permissions/201-post(system).json",
    "/stubs/mod-authn/201-post(system_creds).json",
    "/stubs/mod-authn/201-post(system_login).json",
    "/stubs/mod-inventory-storage/locations/200-get-by-query(dcb).json",
    "/stubs/mod-inventory-storage/holdings-storage/200-get-by-query(dcb+id).json",
    "/stubs/mod-circulation-storage/cancellation-reason-storage/200-get-by-id(dcb).json",
    "/stubs/mod-inventory-storage/loan-types/200-get-by-query(dcb).json",
    "/stubs/mod-calendar/calendars/200-get-all.json"
  })
  void initializeTenant_positive_dcbEntitiesExist() {
    enableTenant();

    assertThatApiIsCalledOnce("/locations");
    assertThatApiIsCalledOnce("/users");
    assertThatApiIsCalledOnce("/holdings-storage/holdings");
    assertThatApiIsCalledOnce("/cancellation-reason-storage/cancellation-reasons");
    assertThatApiIsCalledOnce("/loan-types");
    assertThatApiIsCalledOnce("/calendar/calendars");

    purgeTenant();
  }

  private static void assertThatApiIsCalledOnce(String urlPath) {
    var wiremock = getWireMockClient();
    wiremock.verifyThat(1, requestedFor(HttpMethod.GET.name(), urlPathEqualTo(urlPath)));
  }
}
