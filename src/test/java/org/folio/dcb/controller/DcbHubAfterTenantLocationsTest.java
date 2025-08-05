package org.folio.dcb.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.not;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.folio.dcb.utils.DcbHubLocationsGroupingUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class DcbHubAfterTenantLocationsTest extends BaseIT {

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("application.dcb-hub.locations-url", BaseIT::getOkapiUrl);
    String dcbCredentialsJson = String.format(
      "{\"client_id\": \"client-id-54321\", \"client_secret\": \"client_secret_54321\", \"username\": \"admin54321\", \"password\": \"admin54321\", \"keycloak_url\": \"%s/realms/master/protocol/openid-connect/token\"}",
      getOkapiUrl());
    registry.add("application.secret-store.ephemeral.content.dcb-hub-credentials", () -> dcbCredentialsJson);
  }

  private static final String LOCATIONS_PATH = "/locations";
  private static final String INSTITUTIONS_PATH = "/location-units/institutions";
  private static final String CAMPUSES_PATH = "/location-units/campuses";
  private static final String LIBRARIES_PATH = "/location-units/libraries";
  List<String> unitPaths = List.of(INSTITUTIONS_PATH, CAMPUSES_PATH, LIBRARIES_PATH);

  private static final DcbHubLocationsGroupingUtil.AgencyKey AGENCY_1 = new DcbHubLocationsGroupingUtil.AgencyKey("AG-001", "Agency-One");
  private static final DcbHubLocationsGroupingUtil.AgencyKey AGENCY_2 = new DcbHubLocationsGroupingUtil.AgencyKey("AG-002", "Agency-Two");
  private static final DcbHubLocationsGroupingUtil.AgencyKey AGENCY_3 = new DcbHubLocationsGroupingUtil.AgencyKey("AG-003", "Agency-Three");
  private static final DcbHubLocationsGroupingUtil.AgencyKey AGENCY_4 = new DcbHubLocationsGroupingUtil.AgencyKey("AG-004", "Agency-Four");
  private static final DcbHubLocationsGroupingUtil.AgencyKey AGENCY_5 = new DcbHubLocationsGroupingUtil.AgencyKey("AG-005", "Agency-Five");
  private static final List<DcbHubLocationsGroupingUtil.AgencyKey> AGENCIES = List.of( AGENCY_1, AGENCY_2, AGENCY_3, AGENCY_4, AGENCY_5);

  @Test
  void testHubLocationsInsertedAfterTenantApiCalls1() {
    verifyDcbHubLocationsCalls();
    verifyLocationQueries();
    verifyLocationCreations();
    verifyLocationUnitCreations();
    verifyLocationUnitQueries();
  }

  private void verifyDcbHubLocationsCalls() {
    IntStream.rangeClosed(1, 2).forEach(page ->
      wireMockServer.verify(1, getRequestedFor(urlPathMatching(".*/locations"))
        .withQueryParam("number", equalTo(String.valueOf(page)))
        .withQueryParam("size", equalTo("5"))
        .withHeader(HttpHeaders.AUTHORIZATION, not(absent())))
    );
  }

  private void verifyLocationQueries() {
    IntStream.rangeClosed(1, 8).forEach(i ->
      wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(LOCATIONS_PATH))
        .withQueryParam("query", equalTo(formatLocationQuery(i))))
    );
  }

  private void verifyLocationCreations() {
    IntStream.iterate(2, i -> i <= 8, i -> i + 2).forEach(i ->
      wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(LOCATIONS_PATH))
        .withRequestBody(matchingJsonPath("$.name", equalTo("Location-" + i)))
        .withRequestBody(matchingJsonPath("$.code", equalTo("LOC-" + i))))
    );
  }

  private void verifyLocationUnitCreations() {
    Map<DcbHubLocationsGroupingUtil.AgencyKey, Integer> agencyVerificationCounts = Map.of(
      new DcbHubLocationsGroupingUtil.AgencyKey("AG-002", "Agency-Two"), 2,
      new DcbHubLocationsGroupingUtil.AgencyKey("AG-004", "Agency-Four"), 1
    );

    agencyVerificationCounts.forEach((agency, verificationCount)-> {
      unitPaths.forEach(path ->
        wireMockServer.verify(verificationCount, postRequestedFor(urlPathEqualTo(path))
          .withRequestBody(matchingJsonPath("$.name", equalTo(agency.agencyName())))
          .withRequestBody(matchingJsonPath("$.code", equalTo(agency.agencyCode()))))
      );
    });
  }

  private void verifyLocationUnitQueries() {
    Map<String, Integer> agencyVerificationCounts = Map.of(
      AGENCY_1.agencyName(), 2, AGENCY_2.agencyName(), 2, AGENCY_3.agencyName(), 2,
      AGENCY_4.agencyName(), 1, AGENCY_5.agencyName(), 1
    );

    AGENCIES.forEach(agency -> {
      int expectedCount = agencyVerificationCounts.get(agency.agencyName());
      String query = formatAgencyQuery(agency);

      unitPaths.forEach(path ->
        wireMockServer.verify(expectedCount, getRequestedFor(urlPathEqualTo(path))
          .withQueryParam("query", equalTo(query)))
      );
    });
  }

  private String formatLocationQuery(int index) {
    return String.format("(name==Location-%d and code==LOC-%d)", index, index);
  }

  private String formatAgencyQuery(DcbHubLocationsGroupingUtil.AgencyKey agency) {
    return String.format("(name==%s AND code==%s)", agency.agencyName(), agency.agencyCode());
  }
}
