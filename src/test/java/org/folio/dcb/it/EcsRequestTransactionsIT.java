package org.folio.dcb.it;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.folio.dcb.utils.EntityUtils.BORROWER_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.CIRCULATION_REQUEST_ID;
import static org.folio.dcb.utils.EntityUtils.EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.ITEM_ID;
import static org.folio.dcb.utils.EntityUtils.PATRON_TYPE_USER_ID;
import static org.folio.dcb.utils.EntityUtils.createBorrowingEcsRequestTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createBorrowingPickupEcsRequestTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createLendingEcsRequestTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createPickupEcsRequestTransactionByRole;
import static org.folio.dcb.utils.JsonTestUtils.asJsonString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.folio.dcb.support.types.IntegrationTest;
import org.folio.dcb.support.wiremock.WireMockStub;
import org.folio.dcb.utils.DCBConstants;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class EcsRequestTransactionsIT extends BaseTenantIntegrationTest {

  @Test
  @WireMockStub("/stubs/mod-circulation-storage/request-storage/200-get-by-id(item).json")
  void createLendingEcsRequestTest() throws Exception {
    postEcsRequestTransaction(createLendingEcsRequestTransactionByRole())
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.item.id", is(ITEM_ID)))
      .andExpect(jsonPath("$.item.barcode", is("DCB_ITEM")))
      .andExpect(jsonPath("$.patron.id", is(EXISTED_PATRON_ID)))
      .andExpect(jsonPath("$.patron.barcode", is("DCB_PATRON")));

    //Trying to create another transaction with same transaction id
    postEcsRequestTransactionAttempt(createLendingEcsRequestTransactionByRole())
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(CIRCULATION_REQUEST_ID);
  }

  @Test
  @WireMockStub({
    "/stubs/mod-circulation-storage/request-storage/200-get-by-id(circ-item).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/201-post(borrower).json",
    "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
    "/stubs/mod-circulation/requests/204-put(any).json"
  })
  void createBorrowingEcsRequestTest() throws Exception {
    postEcsRequestTransaction(createBorrowingEcsRequestTransactionByRole())
      .andExpect(status().isCreated());

    wiremock.verifyThat(1, putRequestedFor(urlPathMatching("/circulation/requests/.{36}"))
      .withRequestBody(matchingJsonPath("$.requestType", equalTo("Hold")))
      .withRequestBody(matchingJsonPath("$.itemId", equalTo(ITEM_ID)))
      .withRequestBody(matchingJsonPath("$.status", equalTo("Open - Not yet filled")))
      .withRequestBody(matchingJsonPath("$.instanceId", equalTo(DCBConstants.INSTANCE_ID)))
      .withRequestBody(matchingJsonPath("$.requesterId", equalTo(PATRON_TYPE_USER_ID)))
      .withRequestBody(matchingJsonPath("$.pickupServicePointId", equalTo(BORROWER_SERVICE_POINT_ID)))
      .withRequestBody(matchingJsonPath("$.holdingsRecordId", equalTo(DCBConstants.HOLDING_ID))));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-circulation-storage/request-storage/200-get-by-id(circ-item).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/201-post(borrower).json",
    "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
    "/stubs/mod-circulation/requests/204-put(any).json"
  })
  void createPickupEcsRequestTest() throws Exception {
    postEcsRequestTransaction(createPickupEcsRequestTransactionByRole())
      .andExpect(status().isCreated());

    wiremock.verifyThat(1, putRequestedFor(urlPathMatching("/circulation/requests/.{36}"))
      .withRequestBody(matchingJsonPath("$.requestType", equalTo("Hold")))
      .withRequestBody(matchingJsonPath("$.itemId", equalTo(ITEM_ID)))
      .withRequestBody(matchingJsonPath("$.status", equalTo("Open - Not yet filled")))
      .withRequestBody(matchingJsonPath("$.instanceId", equalTo(DCBConstants.INSTANCE_ID)))
      .withRequestBody(matchingJsonPath("$.requesterId", equalTo(PATRON_TYPE_USER_ID)))
      .withRequestBody(matchingJsonPath("$.pickupServicePointId", equalTo(BORROWER_SERVICE_POINT_ID)))
      .withRequestBody(matchingJsonPath("$.holdingsRecordId", equalTo(DCBConstants.HOLDING_ID))));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-circulation-storage/request-storage/200-get-by-id(circ-item).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/201-post(borrower).json",
    "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
    "/stubs/mod-circulation/requests/204-put(any).json"
  })
  void createBorrowingPickupEcsRequestTest() throws Exception {
    postEcsRequestTransaction(createBorrowingPickupEcsRequestTransactionByRole())
      .andExpect(status().isCreated());

    wiremock.verifyThat(1, putRequestedFor(urlPathMatching("/circulation/requests/.{36}"))
      .withRequestBody(matchingJsonPath("$.requestType", equalTo("Hold")))
      .withRequestBody(matchingJsonPath("$.itemId", equalTo(ITEM_ID)))
      .withRequestBody(matchingJsonPath("$.status", equalTo("Open - Not yet filled")))
      .withRequestBody(matchingJsonPath("$.instanceId", equalTo(DCBConstants.INSTANCE_ID)))
      .withRequestBody(matchingJsonPath("$.requesterId", equalTo(PATRON_TYPE_USER_ID)))
      .withRequestBody(matchingJsonPath("$.pickupServicePointId", equalTo(BORROWER_SERVICE_POINT_ID)))
      .withRequestBody(matchingJsonPath("$.holdingsRecordId", equalTo(DCBConstants.HOLDING_ID))));
  }

  @Test
  @WireMockStub("/stubs/mod-circulation-storage/request-storage/404-get-by-id(item).json")
  void checkErrorStatusForInvalidRequest() throws Exception {
    postEcsRequestTransactionAttempt(createLendingEcsRequestTransactionByRole())
      .andExpectAll(status().is4xxClientError());
  }

  @Test
  @Sql("/db/scripts/ecs_transaction(open).sql")
  void updateTransactionWithItemBarcode() throws Exception {
    String realItemBarcode = "real_item_barcode";
    DcbTransaction requestBody = new DcbTransaction()
      .item(new DcbItem().barcode(realItemBarcode));

    mockMvc.perform(
        patch("/ecs-request-transactions/" + CIRCULATION_REQUEST_ID)
          .content(asJsonString(requestBody))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.item.barcode").value(realItemBarcode));
  }

  @Test
  void updateOfNonExistentTransactionFails() throws Exception {
    DcbTransaction requestBody = new DcbTransaction()
      .item(new DcbItem().barcode("item_barcode"));

    mockMvc.perform(
        patch("/ecs-request-transactions/" + UUID.randomUUID())
          .content(asJsonString(requestBody))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  @SneakyThrows
  protected static ResultActions postEcsRequestTransaction(DcbTransaction dcbTransaction) {
    return postEcsRequestTransactionAttempt(dcbTransaction)
      .andExpect(status().isCreated());
  }

  @SneakyThrows
  protected static ResultActions postEcsRequestTransactionAttempt(DcbTransaction body) {
    return mockMvc.perform(
      post("/ecs-request-transactions/{id}", CIRCULATION_REQUEST_ID)
        .content(asJsonString(body))
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));
  }
}
