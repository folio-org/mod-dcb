package org.folio.dcb.it;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.folio.dcb.utils.EntityUtils.BORROWER_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.ITEM_ID;
import static org.folio.dcb.utils.EntityUtils.PICKUP_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.borrowerDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.borrowingPickupDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.dcbItem;
import static org.folio.dcb.utils.EntityUtils.dcbPatron;
import static org.folio.dcb.utils.EntityUtils.pickupDcbTransaction;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.folio.dcb.it.base.BaseIntegrationTest;
import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.folio.dcb.utils.DCBConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import support.types.IntegrationTest;
import support.wiremock.WireMockStub;

@IntegrationTest
class FlexibleEffectiveLocationIT extends BaseTenantIntegrationTest {

  private static final String SHADOW_LIBRARY_ID = "32188fb2-ac26-42ab-9fd0-1f027e9bf7e2";
  private static final String SHADOW_LOCATION_ID = "e78b9006-c477-4fea-b8e1-5af659948491";
  private static final String DCB_LOCATION_ID = DCBConstants.LOCATION_ID;
  private static final String QUERY_BY_SHADOW_LOCATION_CODE = "code==\"KU\"";

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("application.dcb-hub.fetch-dcb-locations-enabled", () -> true);
    registry.add("application.dcb-hub.locations-url", BaseIntegrationTest::getWiremockUrl);
    registry.add("application.dcb-hub.batch-size", () -> 5);
    registry.add("application.secret-store.ephemeral.content.folio_testtenant_dcb-hub-credentials",
      () -> String.format("""
        {
          "client_id": "test_client_id",
          "client_secret": "test_client_secret",
          "username": "test_openrs_user",
          "password": "test_openrs_password",
          "keycloak_url": "%s/realms/master/protocol/openid-connect/token"
        }
        """.formatted(getWiremockUrl())));
  }

  private static void verifyGetRequestBeingCalledOnce(String exactUrlPath, String query) {
    wiremock.verifyThat(1, getRequestedFor(
      urlPathEqualTo(exactUrlPath)).withQueryParam("query", equalTo(query)));
  }

  private static void verifyPostCirculationItemIsCalledOnce(String effectiveLocationId) {
    wiremock.verifyThat(1, postRequestedFor(urlPathMatching("/circulation-item/.{36}"))
      .withRequestBody(matchingJsonPath("$.effectiveLocationId", equalTo(effectiveLocationId))));
  }

  @SuppressWarnings("SameParameterValue")
  private static void verifyPostCirculationRequestCalledOnce(String requesterId, String servicePointId) {
    wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/circulation/requests"))
      .withRequestBody(matchingJsonPath("$.requestType", equalTo("Hold")))
      .withRequestBody(matchingJsonPath("$.itemId", equalTo(ITEM_ID)))
      .withRequestBody(matchingJsonPath("$.instanceId", equalTo(DCBConstants.INSTANCE_ID)))
      .withRequestBody(matchingJsonPath("$.requesterId", equalTo(requesterId)))
      .withRequestBody(matchingJsonPath("$.pickupServicePointId", equalTo(servicePointId)))
      .withRequestBody(matchingJsonPath("$.holdingsRecordId", equalTo(DCBConstants.HOLDING_ID))));
  }

  @Nested
  @DisplayName("BorrowerRoleIT")
  class BorrowerRoleIT {

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(user).json",
      "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
      "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
      "/stubs/mod-calendar/calendars/200-get-all.json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(KU+shadow).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(borrower+shadow loc).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_locationCodeMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      var dcbItem = dcbItem().locationCode("KU");
      var dcbTransaction = borrowerDcbTransaction(dcbPatron).item(dcbItem);

      postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.item").value(dcbItem))
        .andExpect(jsonPath("$.patron").value(dcbPatron));

      verifyGetRequestBeingCalledOnce("/locations", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyPostCirculationItemIsCalledOnce(SHADOW_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, BORROWER_SERVICE_POINT_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(user).json",
      "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
      "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
      "/stubs/mod-calendar/calendars/200-get-all.json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(KU+shadow empty).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(shadow_library_id).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(KU+shadow).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(borrower+shadow loc).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_locationCodeNotMatchedAndLendingLibraryMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      var dcbItem = dcbItem().locationCode("KU");
      var dcbTransaction = borrowerDcbTransaction(dcbPatron).item(dcbItem);

      postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.item").value(dcbItem))
        .andExpect(jsonPath("$.patron").value(dcbPatron));

      verifyGetRequestBeingCalledOnce("/locations", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyGetRequestBeingCalledOnce("/locations", "libraryId==\"%s\"".formatted(SHADOW_LIBRARY_ID));
      verifyGetRequestBeingCalledOnce("/location-units/libraries", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyPostCirculationItemIsCalledOnce(SHADOW_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, BORROWER_SERVICE_POINT_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(user).json",
      "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
      "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(KU+shadow empty).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(KU+shadow empty).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-calendar/calendars/200-get-all.json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(borrower).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_nothingMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      var dcbItem = dcbItem().locationCode("KU");
      var dcbTransaction = borrowerDcbTransaction(dcbPatron).item(dcbItem);

      postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.item").value(dcbItem))
        .andExpect(jsonPath("$.patron").value(dcbPatron));

      verifyGetRequestBeingCalledOnce("/locations", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyGetRequestBeingCalledOnce("/location-units/libraries", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyPostCirculationItemIsCalledOnce(DCB_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, BORROWER_SERVICE_POINT_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(user).json",
      "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
      "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
      "/stubs/mod-calendar/calendars/200-get-all.json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(KU+shadow).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(shadow_library_id).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(borrower+shadow loc).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_lendingLibraryCodeMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      postDcbTransaction(DCB_TRANSACTION_ID, borrowerDcbTransaction(dcbPatron))
        .andExpect(jsonPath("$.status").value("CREATED"));

      verifyGetRequestBeingCalledOnce("/location-units/libraries", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyGetRequestBeingCalledOnce("/locations", "libraryId==\"%s\"".formatted(SHADOW_LIBRARY_ID));
      verifyPostCirculationItemIsCalledOnce(SHADOW_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, BORROWER_SERVICE_POINT_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(user).json",
      "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
      "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
      "/stubs/mod-calendar/calendars/200-get-all.json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(KU+shadow empty).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(borrower).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_lendingLibraryCodeNotMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      postDcbTransaction(DCB_TRANSACTION_ID, borrowerDcbTransaction(dcbPatron))
        .andExpect(jsonPath("$.status").value("CREATED"));

      verifyGetRequestBeingCalledOnce("/location-units/libraries", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyPostCirculationItemIsCalledOnce(DCB_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, BORROWER_SERVICE_POINT_ID);
    }
  }

  @Nested
  @DisplayName("BorrowerPickupRoleIT")
  class BorrowerPickupRoleIT {

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(user).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(KU+shadow).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(pickup+shadow loc).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_locationCodeMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      var dcbItem = dcbItem().locationCode("KU");
      var dcbTransaction = borrowingPickupDcbTransaction(dcbPatron).item(dcbItem);
      postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.item").value(dcbItem))
        .andExpect(jsonPath("$.patron").value(dcbPatron));

      verifyGetRequestBeingCalledOnce("/locations", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyPostCirculationItemIsCalledOnce(SHADOW_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, PICKUP_SERVICE_POINT_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(user).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(KU+shadow empty).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(shadow_library_id).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(KU+shadow).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(pickup+shadow loc).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_locationCodeNotMatchedAndLendingLibraryMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      var dcbItem = dcbItem().locationCode("KU");
      var dcbTransaction = borrowingPickupDcbTransaction(dcbPatron).item(dcbItem);
      postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.item").value(dcbItem))
        .andExpect(jsonPath("$.patron").value(dcbPatron));

      verifyGetRequestBeingCalledOnce("/locations", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyGetRequestBeingCalledOnce("/locations", "libraryId==\"%s\"".formatted(SHADOW_LIBRARY_ID));
      verifyGetRequestBeingCalledOnce("/location-units/libraries", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyPostCirculationItemIsCalledOnce(SHADOW_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, PICKUP_SERVICE_POINT_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(user).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(KU+shadow empty).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(KU+shadow empty).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(pickup).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_nothingMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      var dcbItem = dcbItem().locationCode("KU");
      var dcbTransaction = borrowingPickupDcbTransaction(dcbPatron).item(dcbItem);
      postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.item").value(dcbItem))
        .andExpect(jsonPath("$.patron").value(dcbPatron));

      verifyGetRequestBeingCalledOnce("/locations", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyGetRequestBeingCalledOnce("/location-units/libraries", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyPostCirculationItemIsCalledOnce(DCB_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, PICKUP_SERVICE_POINT_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(user).json",
      "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
      "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
      "/stubs/mod-calendar/calendars/200-get-all.json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(KU+shadow).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(shadow_library_id).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(borrower+shadow loc).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_lendingLibraryCodeMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      postDcbTransaction(DCB_TRANSACTION_ID, borrowerDcbTransaction(dcbPatron))
        .andExpect(jsonPath("$.status").value("CREATED"));

      verifyGetRequestBeingCalledOnce("/location-units/libraries", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyGetRequestBeingCalledOnce("/locations", "libraryId==\"%s\"".formatted(SHADOW_LIBRARY_ID));
      verifyPostCirculationItemIsCalledOnce(SHADOW_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, BORROWER_SERVICE_POINT_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(user).json",
      "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
      "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
      "/stubs/mod-calendar/calendars/200-get-all.json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(KU+shadow empty).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(borrower).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_lendingLibraryCodeNotMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      postDcbTransaction(DCB_TRANSACTION_ID, borrowerDcbTransaction(dcbPatron))
        .andExpect(jsonPath("$.status").value("CREATED"));

      verifyGetRequestBeingCalledOnce("/location-units/libraries", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyPostCirculationItemIsCalledOnce(DCB_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, BORROWER_SERVICE_POINT_ID);
    }
  }

  @Nested
  @DisplayName("PickupRoleIT")
  class PickupRoleIT {

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(dcb user).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(KU+shadow).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(pickup+shadow loc).json",
      "/stubs/mod-users/groups/200-get-by-query(staff).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_locationCodeMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      var dcbItem = dcbItem().locationCode("KU");
      var dcbTransaction = pickupDcbTransaction(dcbPatron).item(dcbItem);
      postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.item").value(dcbItem))
        .andExpect(jsonPath("$.patron").value(dcbPatron));

      verifyGetRequestBeingCalledOnce("/locations", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyPostCirculationItemIsCalledOnce(SHADOW_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, PICKUP_SERVICE_POINT_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(dcb user).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(KU+shadow empty).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(shadow_library_id).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(KU+shadow).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(pickup+shadow loc).json",
      "/stubs/mod-users/groups/200-get-by-query(staff).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_locationCodeNotMatchedAndLendingLibraryMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      var dcbItem = dcbItem().locationCode("KU");
      var dcbTransaction = pickupDcbTransaction(dcbPatron).item(dcbItem);
      postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.item").value(dcbItem))
        .andExpect(jsonPath("$.patron").value(dcbPatron));

      verifyGetRequestBeingCalledOnce("/locations", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyGetRequestBeingCalledOnce("/location-units/libraries", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyPostCirculationItemIsCalledOnce(SHADOW_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, PICKUP_SERVICE_POINT_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(dcb user).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(KU+shadow empty).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(KU+shadow empty).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(pickup).json",
      "/stubs/mod-users/groups/200-get-by-query(staff).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_nothingMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      var dcbItem = dcbItem().locationCode("KU");
      var dcbTransaction = pickupDcbTransaction(dcbPatron).item(dcbItem);
      postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.item").value(dcbItem))
        .andExpect(jsonPath("$.patron").value(dcbPatron));

      verifyGetRequestBeingCalledOnce("/locations", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyGetRequestBeingCalledOnce("/location-units/libraries", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyPostCirculationItemIsCalledOnce(DCB_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, PICKUP_SERVICE_POINT_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(dcb user).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(shadow_library_id).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(KU+shadow).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(pickup+shadow loc).json",
      "/stubs/mod-users/groups/200-get-by-query(staff).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_lendingLibraryCodeMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      postDcbTransaction(DCB_TRANSACTION_ID, pickupDcbTransaction(dcbPatron))
        .andExpect(jsonPath("$.status").value("CREATED"));

      verifyGetRequestBeingCalledOnce("/location-units/libraries", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyGetRequestBeingCalledOnce("/locations", "libraryId==\"%s\"".formatted(SHADOW_LIBRARY_ID));
      verifyPostCirculationItemIsCalledOnce(SHADOW_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, PICKUP_SERVICE_POINT_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(dcb user).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(KU+shadow empty).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/201-post(pickup).json",
      "/stubs/mod-users/groups/200-get-by-query(staff).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
    })
    void createTransaction_positive_lendingLibraryCodeNotMatched() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      postDcbTransaction(DCB_TRANSACTION_ID, pickupDcbTransaction(dcbPatron))
        .andExpect(jsonPath("$.status").value("CREATED"));

      verifyGetRequestBeingCalledOnce("/location-units/libraries", QUERY_BY_SHADOW_LOCATION_CODE);
      verifyPostCirculationItemIsCalledOnce(DCB_LOCATION_ID);
      verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID, PICKUP_SERVICE_POINT_ID);
    }
  }

  @Nested
  @DisplayName("ShadowLocationRefreshIT")
  class ShadowLocationRefreshIT {

    @Test
    @WireMockStub({
      "/stubs/dcb-hub/keycloak/200-get-auth-token.json",
      "/stubs/dcb-hub/locations/200-get-all(2 pages).json",
      "/stubs/mod-inventory-storage/locations/200-get-all(shadow+found_by_codes).json",
      "/stubs/mod-inventory-storage/locations/200-get-all(shadow+empty_by_codes).json",
      "/stubs/mod-inventory-storage/location-units/institutions/200-get-by-query(shadow+name+code).json",
      "/stubs/mod-inventory-storage/location-units/institutions/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/campuses/200-get-by-query(shadow+name+code).json",
      "/stubs/mod-inventory-storage/location-units/campuses/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(shadow+name+code).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/institutions/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/location-units/campuses/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/location-units/libraries/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/locations/201-post(any shadow).json",
    })
    void refreshShadowLocations_positive() throws Exception {
      refreshShadowLocations()
        .andExpect(jsonPath("$.locations[?(@.code=='LOC-1')].status").value("SUCCESS"))
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
    @WireMockStub({
      "/stubs/dcb-hub/keycloak/200-get-auth-token.json",
      "/stubs/dcb-hub/locations/200-get-all(single).json",
      "/stubs/mod-inventory-storage/location-units/institutions/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/institutions/400-post(AG-2).json",
    })
    void refreshShadowLocations_validateExceptionWhenInstitutionCreate() throws Exception {
      refreshShadowLocations()
        .andExpect(jsonPath("$.locations[?(@.code=='LOC-2')].status").value("SKIPPED"))
        .andExpect(jsonPath("$.location-units.institutions[?(@.code=='AG-002')].status").value("ERROR"))
        .andExpect(jsonPath("$.location-units.institutions[?(@.code=='AG-002')].cause").value(hasItem(allOf(
          startsWith("[400 Bad Request]"), containsString("Institution with code AG-004 already exists")))))
        .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-002')].status").value("SKIPPED"))
        .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-002')].cause").value(
          "Institution is null and it was not created, so cannot create campus"))
        .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-002')].status").value("SKIPPED"))
        .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-002')].cause").value(
          "Campus is null and it was not created, so cannot create library"));
    }

    @Test
    @WireMockStub({
      "/stubs/dcb-hub/keycloak/200-get-auth-token.json",
      "/stubs/dcb-hub/locations/200-get-all(single).json",
      "/stubs/mod-inventory-storage/location-units/institutions/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/institutions/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/location-units/campuses/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/campuses/400-post(AG-2).json",
    })
    void refreshShadowLocations_validateExceptionWhenCampusCreate() throws Exception {
      refreshShadowLocations()
        .andExpect(jsonPath("$.locations[?(@.code=='LOC-2')].status").value("SKIPPED"))
        .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-002')].status").value("ERROR"))
        .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-002')].cause").value(hasItem(allOf(
          startsWith("[400 Bad Request]"), containsString("Campus with code AG-004 already exists")))))
        .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-002')].status").value("SKIPPED"))
        .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-002')].cause").value(
          "Campus is null and it was not created, so cannot create library"))
        .andExpect(jsonPath("$.location-units.institutions[?(@.code=='AG-002')].status").value("SUCCESS"));
    }

    @Test
    @WireMockStub({
      "/stubs/dcb-hub/keycloak/200-get-auth-token.json",
      "/stubs/dcb-hub/locations/200-get-all(single).json",
      "/stubs/mod-inventory-storage/location-units/institutions/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/institutions/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/location-units/campuses/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/campuses/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/libraries/400-post(AG-2).json",
    })
    void refreshShadowLocations_validateExceptionWhenLibraryCreate() throws Exception {
      refreshShadowLocations()
        .andExpect(jsonPath("$.locations[?(@.code=='LOC-2')].status").value("SKIPPED"))
        .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-002')].status").value("ERROR"))
        .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-002')].cause").value(hasItem(allOf(
          startsWith("[400 Bad Request]"), containsString("Library with code AG-004 already exists")))));
    }

    @Test
    @WireMockStub({
      "/stubs/dcb-hub/keycloak/200-get-auth-token.json",
      "/stubs/dcb-hub/locations/200-get-all(single).json",
      "/stubs/mod-inventory-storage/location-units/institutions/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/institutions/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/location-units/campuses/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/campuses/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/libraries/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/locations/200-get-all(shadow+empty_by_codes).json",
      "/stubs/mod-inventory-storage/locations/400-post(AG-2).json",
    })
    void refreshShadowLocations_validateExceptionWhenLocationCreate() throws Exception {
      refreshShadowLocationsAttempt()
        .andExpect(jsonPath("$.locations[?(@.code=='LOC-2')].status").value("ERROR"))
        .andExpect(jsonPath("$.locations[?(@.code=='LOC-2')].cause").value(hasItem(allOf(
          startsWith("[400 Bad Request]"), containsString("Location with code AG-004 already exists")))));
    }

    @Test
    @WireMockStub({
      "/stubs/dcb-hub/keycloak/200-get-auth-token.json",
      "/stubs/dcb-hub/locations/200-get-all(empty).json",
    })
    void refreshShadowLocations_validate400WhenNoLocationsDataReceivedFromDcbHub() throws Exception {
      refreshShadowLocations()
        .andExpect(jsonPath("$.locations").exists())
        .andExpect(jsonPath("$.location-units").doesNotExist());
    }
  }
}
