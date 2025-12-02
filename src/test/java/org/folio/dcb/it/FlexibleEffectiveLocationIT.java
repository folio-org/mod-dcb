package org.folio.dcb.it;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Collections.emptyList;
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
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;
import org.folio.dcb.domain.dto.DcbAgency;
import org.folio.dcb.domain.dto.DcbLocation;
import org.folio.dcb.domain.dto.ShadowLocationRefreshBody;
import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.folio.dcb.utils.DCBConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.TestPropertySource;
import support.types.IntegrationTest;
import support.wiremock.WireMockStub;

@IntegrationTest
@TestPropertySource(properties = { "application.features.flexible-circulation-rules-enabled=true" })
class FlexibleEffectiveLocationIT extends BaseTenantIntegrationTest {

  private static final String SHADOW_LIBRARY_ID = "32188fb2-ac26-42ab-9fd0-1f027e9bf7e2";
  private static final String SHADOW_LOCATION_ID = "e78b9006-c477-4fea-b8e1-5af659948491";
  private static final String DCB_LOCATION_ID = DCBConstants.LOCATION_ID;
  private static final String QUERY_BY_SHADOW_LOCATION_CODE = "code==\"KU\"";

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
      refreshShadowLocations(defaultRefreshBody())
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
      "/stubs/mod-inventory-storage/locations/200-get-all(shadow+empty_by_codes).json",
      "/stubs/mod-inventory-storage/location-units/institutions/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/campuses/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/institutions/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/location-units/campuses/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/location-units/libraries/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/locations/201-post(any shadow).json",
    })
    void refreshShadowLocations_positive_onlyAgency() throws Exception {
      var agency = dcbAgency("AG-002", "Agency-Two");
      refreshShadowLocations(refreshBody(emptyList(), List.of(agency)))
        .andExpect(jsonPath("$.locations[?(@.code=='AG-002')].status").value("SUCCESS"))
        .andExpect(jsonPath("$.location-units.institutions[?(@.code=='AG-002')].status").value("SUCCESS"))
        .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-002')].status").value("SUCCESS"))
        .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-002')].status").value("SUCCESS"));
    }

    @Test
    @WireMockStub({
      "/stubs/mod-inventory-storage/location-units/institutions/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/institutions/400-post(AG-2).json",
    })
    void refreshShadowLocations_validateExceptionWhenInstitutionCreate() throws Exception {
      var location = dcbLocation("LOC-2", "Location-2", dcbAgency("AG-002", "Agency-Two"));
      refreshShadowLocations(refreshBody(List.of(location), emptyList()))
        .andExpect(jsonPath("$.locations[?(@.code=='LOC-2')].status").value("SKIPPED"))
        .andExpect(jsonPath("$.location-units.institutions[?(@.code=='AG-002')].status").value("ERROR"))
        .andExpect(jsonPath("$.location-units.institutions[?(@.code=='AG-002')].cause").value(hasItem(allOf(
          startsWith("[400 Bad Request]"), containsString("Institution with code AG-004 already exists")))))
        .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-002')].status").value("SKIPPED"))
        .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-002')].cause").value(
          "Parent institution is not created"))
        .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-002')].status").value("SKIPPED"))
        .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-002')].cause").value(
          "Parent campus is not created"));
    }

    @Test
    @WireMockStub({
      "/stubs/mod-inventory-storage/location-units/institutions/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/institutions/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/location-units/campuses/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/campuses/400-post(AG-2).json",
    })
    void refreshShadowLocations_validateExceptionWhenCampusCreate() throws Exception {
      var location = dcbLocation("LOC-2", "Location-2", dcbAgency("AG-002", "Agency-Two"));
      refreshShadowLocations(refreshBody(List.of(location), emptyList()))
        .andExpect(jsonPath("$.locations[?(@.code=='LOC-2')].status").value("SKIPPED"))
        .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-002')].status").value("ERROR"))
        .andExpect(jsonPath("$.location-units.campuses[?(@.code=='AG-002')].cause").value(hasItem(allOf(
          startsWith("[400 Bad Request]"), containsString("Campus with code AG-004 already exists")))))
        .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-002')].status").value("SKIPPED"))
        .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-002')].cause").value("Parent campus is not created"))
        .andExpect(jsonPath("$.location-units.institutions[?(@.code=='AG-002')].status").value("SUCCESS"));
    }

    @Test
    @WireMockStub({
      "/stubs/mod-inventory-storage/location-units/institutions/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/institutions/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/location-units/campuses/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/campuses/201-post(any shadow).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(shadow+name+code empty).json",
      "/stubs/mod-inventory-storage/location-units/libraries/400-post(AG-2).json",
    })
    void refreshShadowLocations_validateExceptionWhenLibraryCreate() throws Exception {
      var location = dcbLocation("LOC-2", "Location-2", dcbAgency("AG-002", "Agency-Two"));
      refreshShadowLocations(refreshBody(List.of(location), emptyList()))
        .andExpect(jsonPath("$.locations[?(@.code=='LOC-2')].status").value("SKIPPED"))
        .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-002')].status").value("ERROR"))
        .andExpect(jsonPath("$.location-units.libraries[?(@.code=='AG-002')].cause").value(hasItem(allOf(
          startsWith("[400 Bad Request]"), containsString("Library with code AG-004 already exists")))));
    }

    @Test
    @WireMockStub({
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
      var location = dcbLocation("LOC-2", "Location-2", dcbAgency("AG-002", "Agency-Two"));
      refreshShadowLocationsAttempt(refreshBody(List.of(location), emptyList()))
        .andExpect(jsonPath("$.locations[?(@.code=='LOC-2')].status").value("ERROR"))
        .andExpect(jsonPath("$.locations[?(@.code=='LOC-2')].cause").value(hasItem(allOf(
          startsWith("[400 Bad Request]"), containsString("Location with code AG-004 already exists")))));
    }

    @Test
    void refreshShadowLocations_positive_emptyRequestBody() throws Exception {
      refreshShadowLocations(refreshBody(emptyList(), emptyList()))
        .andExpect(jsonPath("$.locations").exists())
        .andExpect(jsonPath("$.location-units").doesNotExist());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidRefreshBodyProviders")
    void refreshShadowLocations_negativeParameterized_invalidInputValues(
      @SuppressWarnings("unused") String name, ShadowLocationRefreshBody requestBody) throws Exception {
      refreshShadowLocationsAttempt(requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }

    private static ShadowLocationRefreshBody defaultRefreshBody() {
      return new ShadowLocationRefreshBody()
        .addLocationsItem(dcbLocation("LOC-1", "Location-1", dcbAgency("AG-001", "Agency-One")))
        .addLocationsItem(dcbLocation("LOC-2", "Location-2", dcbAgency("AG-002", "Agency-Two")))
        .addLocationsItem(dcbLocation("LOC-3", "Location-3", dcbAgency("AG-003", "Agency-Three")))
        .addLocationsItem(dcbLocation("LOC-4", "Location-4", dcbAgency("AG-004", "Agency-Four")))
        .addLocationsItem(dcbLocation("LOC-5", "Location-5", dcbAgency("AG-005", "Agency-Five")))
        .addLocationsItem(dcbLocation("LOC-6", "Location-6", dcbAgency("AG-001", "Agency-One")))
        .addLocationsItem(dcbLocation("LOC-7", "Location-7", dcbAgency("AG-002", "Agency-Two")))
        .addLocationsItem(dcbLocation("LOC-8", "Location-8", dcbAgency("AG-003", "Agency-Three")));
    }

    private static ShadowLocationRefreshBody refreshBody(List<DcbLocation> locations, List<DcbAgency> agencies) {
      return new ShadowLocationRefreshBody().locations(locations).agencies(agencies);
    }

    private static DcbLocation dcbLocation(String code, String name, DcbAgency agency) {
      return new DcbLocation().name(name).code(code).agency(agency);
    }

    private static DcbAgency dcbAgency(String code, String name) {
      return new DcbAgency().name(name).code(code);
    }

    private static Stream<Arguments> invalidRefreshBodyProviders() {
      return Stream.of(
        arguments("Empty location code", refreshBody(
          List.of(dcbLocation(null, "Loc", dcbAgency("AG1", "test"))), emptyList())),
        arguments("Empty location name", refreshBody(
          List.of(dcbLocation("LC", null, dcbAgency("AG1", "test"))), emptyList())),
        arguments("Null agency in location", refreshBody(
          List.of(dcbLocation("LC", "Loc", null)), emptyList())),
        arguments("Empty agency code in location", refreshBody(
          List.of(dcbLocation("LC", "Loc", dcbAgency(null, "test"))), emptyList())),
        arguments("Empty agency name in location", refreshBody(
          List.of(dcbLocation("LC", "Loc", dcbAgency("AG1", null))), emptyList())),
        arguments("Empty agency code", refreshBody(emptyList(), List.of(dcbAgency(null, "test")))),
        arguments("Empty agency name", refreshBody(emptyList(), List.of(dcbAgency("AG1", null))))
      );
    }
  }
}
