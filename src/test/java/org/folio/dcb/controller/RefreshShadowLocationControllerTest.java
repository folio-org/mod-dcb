package org.folio.dcb.controller;

import static org.folio.dcb.utils.DCBConstants.NAME;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.folio.dcb.client.feign.DcbHubLocationClient;
import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.client.feign.LocationUnitClient;
import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.config.DcbHubProperties;
import org.folio.dcb.model.DcbHubLocationResponse;
import org.folio.dcb.service.DcbHubLocationService;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

class RefreshShadowLocationControllerTest extends BaseIT {

  private static final String SHADOW_LOCATIONS_REFRESH_ENDPOINT = "/dcb/shadow-locations/refresh";

  @MockitoSpyBean
  private DcbHubLocationService dcbHubLocationService;
  @MockitoSpyBean
  private InventoryServicePointClient servicePointClient;
  @MockitoSpyBean
  private DcbHubLocationClient dcbHubLocationClient;
  @MockitoSpyBean
  private LocationUnitClient locationUnitClient;
  @MockitoSpyBean
  private LocationsClient locationsClient;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("application.dcb-hub.fetch-dcb-locations-enabled", () -> false);
    registry.add("application.dcb-hub.locations-url", BaseIT::getOkapiUrl);
    String dcbCredentialsJson = String.format("""
    {
      "client_id": "client-id-54321",
      "client_secret": "client_secret_54321",
      "username": "admin54321",
      "password": "admin54321",
      "keycloak_url": "%s/realms/master/protocol/openid-connect/token"
    }
    """, getOkapiUrl());
    registry.add("application.secret-store.ephemeral.content.folio_diku_dcb-hub-credentials", () -> dcbCredentialsJson);
  }

  @Test
  void testRefreshShadowLocations_positive() throws Exception {
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(true);
    ReflectionTestUtils.setField(dcbHubLocationService, "dcbHubProperties", dcbHubProperties);

    this.mockMvc.perform(
        post(SHADOW_LOCATIONS_REFRESH_ENDPOINT)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-1')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-2')].status").value("SUCCESS"))
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-3')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-4')].status").value("SUCCESS"))
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-5')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-6')].status").value("SUCCESS"))
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-7')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-8')].status").value("SUCCESS"))
      .andExpect(jsonPath("$.location-units.institutions[?(@.code=='AG-001')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.location-units.institutions[?(@.code=='AG-002')].status").value("SUCCESS"))
      .andExpect(jsonPath("$.location-units.institutions[?(@.code=='AG-003')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.location-units.institutions[?(@.code=='AG-004')].status").value("SUCCESS"))
      .andExpect(jsonPath("$.location-units.institutions[?(@.code=='AG-005')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-001')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-002')].status").value("SUCCESS"))
      .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-003')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-004')].status").value("SUCCESS"))
      .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-005')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-001')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-002')].status").value("SUCCESS"))
      .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-003')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-004')].status").value("SUCCESS"))
      .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-005')].status").value("SKIPPED"));
  }

  @Test
  void testRefreshShadowLocations_validateExceptionWhenInstitutionCreate() throws Exception {
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(true);
    ReflectionTestUtils.setField(dcbHubLocationService, "dcbHubProperties", dcbHubProperties);

    doThrow(new RuntimeException("FORCED_EXCEPTION_OCCURRED")).when(locationUnitClient).findInstitutionsByQuery(contains("AG-001"), eq(true), anyInt(), anyInt());

    this.mockMvc.perform(
        post(SHADOW_LOCATIONS_REFRESH_ENDPOINT)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-1')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-6')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.location-units.institutions[?(@.code=='AG-001')].status").value("ERROR"))
      .andExpect(jsonPath("$.location-units.institutions[?(@.code=='AG-001')].cause").value("FORCED_EXCEPTION_OCCURRED"))
      .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-001')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-001')].cause").value("Institution is null and it was not created, so cannot create campus"))
      .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-001')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-001')].cause").value("Campus is null and it was not created, so cannot create library"));
  }

  @Test
  void testRefreshShadowLocations_validateExceptionWhenCampusCreate() throws Exception {
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(true);
    ReflectionTestUtils.setField(dcbHubLocationService, "dcbHubProperties", dcbHubProperties);

    doThrow(new RuntimeException("FORCED_EXCEPTION_OCCURRED")).when(locationUnitClient).findCampusesByQuery(contains("AG-001"), eq(true), anyInt(), anyInt());

    this.mockMvc.perform(
        post(SHADOW_LOCATIONS_REFRESH_ENDPOINT)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-1')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-6')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-001')].status").value("ERROR"))
      .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-001')].cause").value("FORCED_EXCEPTION_OCCURRED"))
      .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-001')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-001')].cause").value("Campus is null and it was not created, so cannot create library"));
  }

  @Test
  void testRefreshShadowLocations_validateExceptionWhenLibraryCreate() throws Exception {
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(true);
    ReflectionTestUtils.setField(dcbHubLocationService, "dcbHubProperties", dcbHubProperties);

    doThrow(new RuntimeException("FORCED_EXCEPTION_OCCURRED")).when(locationUnitClient).findLibrariesByQuery(contains("AG-001"), eq(true), anyInt(), anyInt());

    this.mockMvc.perform(
        post(SHADOW_LOCATIONS_REFRESH_ENDPOINT)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-1')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-6')].status").value("SKIPPED"))
      .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-001')].status").value("ERROR"))
      .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-001')].cause").value("FORCED_EXCEPTION_OCCURRED"));
  }

  @Test
  void testRefreshShadowLocations_validateExceptionWhenLocationCreate() throws Exception {
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(true);
    ReflectionTestUtils.setField(dcbHubLocationService, "dcbHubProperties", dcbHubProperties);

    doThrow(new RuntimeException("FORCED_EXCEPTION_OCCURRED")).when(locationsClient).findLocationByQuery(contains("LOC-1"), eq(true), anyInt(), anyInt());

    this.mockMvc.perform(
        post(SHADOW_LOCATIONS_REFRESH_ENDPOINT)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-1')].status").value("ERROR"))
      .andExpect(jsonPath("$.locations[?(@.code=='LOC-1')].cause").value("FORCED_EXCEPTION_OCCURRED"));
  }

  @Test
  void testRefreshShadowLocations_validate400WhenFetchDcbHubLocationDisabled() throws Exception {
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(false);
    ReflectionTestUtils.setField(dcbHubLocationService, "dcbHubProperties", dcbHubProperties);

    doReturn(ResultList.of(0, Collections.emptyList())).when(servicePointClient).getServicePointByName(NAME);

    this.mockMvc.perform(
        post(SHADOW_LOCATIONS_REFRESH_ENDPOINT)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].message", containsString("DCB Hub locations fetching is disabled, skipping shadow location creation")));
  }

  @Test
  void testRefreshShadowLocations_validate400WhenNoLocationsDataReceivedFromDcbHub() throws Exception {
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(true);
    ReflectionTestUtils.setField(dcbHubLocationService, "dcbHubProperties", dcbHubProperties);
    DcbHubLocationResponse dcbHubLocationResponse = new DcbHubLocationResponse();
    dcbHubLocationResponse.setContent(Collections.emptyList());
    dcbHubLocationResponse.setTotalPages(0);
    doReturn(dcbHubLocationResponse).when(dcbHubLocationClient).getLocations(anyInt(), anyInt(), any());

    this.mockMvc.perform(
        post(SHADOW_LOCATIONS_REFRESH_ENDPOINT)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.locations").doesNotExist())
      .andExpect(jsonPath("$.location-units").doesNotExist());
  }

  @Test
  void testRefreshShadowLocations_validate404dWhenDCBServicePointNotExist() throws Exception {
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(true);
    dcbHubProperties.setLocationsUrl(getOkapiUrl());
    ReflectionTestUtils.setField(dcbHubLocationService, "dcbHubProperties", dcbHubProperties);

    doReturn(ResultList.of(0, Collections.emptyList())).when(servicePointClient).getServicePointByName(NAME);

    this.mockMvc.perform(
        post(SHADOW_LOCATIONS_REFRESH_ENDPOINT)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.errors[0].message", containsString("DCB Service point is not found")));
  }
}