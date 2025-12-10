package org.folio.dcb.it.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.support.wiremock.WiremockContainerExtension.WM_URL_PROPERTY;
import static org.folio.dcb.utils.EntityUtils.REQUEST_USER_ID;
import static org.folio.dcb.utils.EntityUtils.TEST_TENANT;
import static org.folio.dcb.utils.JsonTestUtils.asJsonString;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;
import static org.folio.spring.integration.XOkapiHeaders.URL;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import lombok.SneakyThrows;
import org.folio.dcb.domain.dto.ShadowLocationRefreshBody;
import org.folio.dcb.support.kafka.WithKafkaContainer;
import org.folio.dcb.support.postgres.WithPostgresContainer;
import org.folio.dcb.support.wiremock.WithWiremockContainer;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@ActiveProfiles("it")
@AutoConfigureMockMvc
@WithKafkaContainer
@WithPostgresContainer
@WithWiremockContainer
@SqlMergeMode(MERGE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest {

  protected static final String MODULE_NAME = "mod-dcb";
  protected static final String TEST_TOKEN = "dGVzdF9qd3RfdG9rZW4=";

  protected static MockMvc mockMvc;

  @SneakyThrows
  protected static void enableTenant() {
    enableTenant(TEST_TENANT, new TenantAttributes());
  }

  @SneakyThrows
  protected static void purgeTenant() {
    purgeTenant(TEST_TENANT);
  }

  @BeforeAll
  static void setupMockMvc(@Autowired MockMvc mockMvc) {
    BaseIntegrationTest.mockMvc = mockMvc;
  }

  @SneakyThrows
  @SuppressWarnings("SameParameterValue")
  protected static void enableTenant(String tenant, TenantAttributes tenantAttributes) {
    tenantAttributes.moduleTo(MODULE_NAME);
    mockMvc.perform(post("/_/tenant")
        .content(asJsonString(tenantAttributes))
        .contentType(APPLICATION_JSON)
        .header(TENANT, tenant)
        .header(URL, System.getProperty(WM_URL_PROPERTY)))
      .andExpect(status().isNoContent());
  }

  @SneakyThrows
  @SuppressWarnings("SameParameterValue")
  protected static void purgeTenant(String tenantId) {
    var tenantAttributes = new TenantAttributes().moduleFrom(MODULE_NAME).purge(true);
    mockMvc.perform(post("/_/tenant")
        .content(asJsonString(tenantAttributes))
        .contentType(APPLICATION_JSON)
        .header(TENANT, tenantId)
        .header(TOKEN, TEST_TOKEN))
      .andExpect(status().isNoContent());
  }

  protected static HttpHeaders defaultHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.put(TENANT, List.of(TEST_TENANT));
    httpHeaders.add(URL, getWiremockUrl());
    httpHeaders.add(TOKEN, TEST_TOKEN);
    httpHeaders.add(USER_ID, REQUEST_USER_ID);

    return httpHeaders;
  }

  protected static String getWiremockUrl() {
    var wiremockUrl = System.getProperty(WM_URL_PROPERTY);
    assertThat(wiremockUrl).isNotBlank();
    return wiremockUrl;
  }

  @SneakyThrows
  protected static ResultActions refreshShadowLocations(ShadowLocationRefreshBody refreshBody) {
    return refreshShadowLocationsAttempt(refreshBody).andExpect(status().isCreated());
  }

  @SneakyThrows
  protected static ResultActions refreshShadowLocationsAttempt(ShadowLocationRefreshBody refreshBody) {
    return mockMvc.perform(
      post("/dcb/shadow-locations/refresh")
        .content(asJsonString(refreshBody))
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));
  }
}
