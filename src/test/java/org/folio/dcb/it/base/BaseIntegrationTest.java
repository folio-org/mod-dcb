package org.folio.dcb.it.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;
import static org.folio.spring.integration.XOkapiHeaders.URL;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static support.wiremock.WiremockContainerExtension.WM_URL_PROPERTY;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.SneakyThrows;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import support.kafka.WithKafkaContainer;
import support.postgres.WithPostgresContainer;
import support.wiremock.WithWiremockContainer;

@SpringBootTest
@ActiveProfiles("it")
@AutoConfigureMockMvc
@WithKafkaContainer
@WithPostgresContainer
@WithWiremockContainer
@SqlMergeMode(MERGE)
public class BaseIntegrationTest {

  protected static final String MODULE_NAME = "mod-dcb";
  protected static final String TEST_TENANT = "test_tenant";
  protected static final String TEST_TOKEN = "dGVzdF9qd3RfdG9rZW4=";
  protected static final String TEST_USER_ID = "08d51c7a-0f36-4f3d-9e35-d285612a23df";

  protected static MockMvc mockMvc;
  protected static ObjectMapper objectMapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL);

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

  @SneakyThrows
  public static String asJsonString(Object value) {
    return objectMapper.writeValueAsString(value);
  }

  protected static HttpHeaders defaultHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.put(TENANT, List.of(TEST_TENANT));
    httpHeaders.add(URL, getWiremockUrl());
    httpHeaders.add(TOKEN, TEST_TOKEN);
    httpHeaders.add(USER_ID, TEST_USER_ID);

    return httpHeaders;
  }

  protected static String getWiremockUrl() {
    var wiremockUrl = System.getProperty(WM_URL_PROPERTY);
    assertThat(wiremockUrl).isNotBlank();
    return wiremockUrl;
  }

  @SneakyThrows
  protected static ResultActions refreshShadowLocations() {
    return refreshShadowLocationsAttempt().andExpect(status().isCreated());
  }

  @SneakyThrows
  protected static ResultActions refreshShadowLocationsAttempt() {
    return mockMvc.perform(
      post("/dcb/shadow-locations/refresh")
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));
  }
}
