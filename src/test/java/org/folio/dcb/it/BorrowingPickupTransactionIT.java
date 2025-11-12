package org.folio.dcb.it;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CANCELLED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;
import static org.folio.dcb.utils.EntityUtils.BORROWER_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.ITEM_ID;
import static org.folio.dcb.utils.EntityUtils.NOT_EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.PATRON_TYPE_USER_ID;
import static org.folio.dcb.utils.EntityUtils.PICKUP_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.borrowingPickupDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.dcbItem;
import static org.folio.dcb.utils.EntityUtils.dcbTransactionUpdate;
import static org.folio.dcb.utils.EntityUtils.transactionStatus;
import static org.folio.dcb.utils.EntityUtils.dcbPatron;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.folio.dcb.utils.DCBConstants;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import support.types.IntegrationTest;
import support.wiremock.WireMockStub;

@IntegrationTest
class BorrowingPickupTransactionIT extends BaseTenantIntegrationTest {

  @Test
  @WireMockStub({
    "/stubs/mod-users/users/200-get-by-query(user).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
    "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
    "/stubs/mod-circulation-item/201-post(pickup).json",
    "/stubs/mod-circulation/requests/201-post(any).json"
  })
  void createTransaction_positive_newDcbItem() throws Exception {
    var patron = dcbPatron(EXISTED_PATRON_ID);
    postDcbTransaction(DCB_TRANSACTION_ID, borrowingPickupDcbTransaction(patron))
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(patron));

    wiremock.verifyThat(0, getRequestedFor(urlPathEqualTo("/locations")));
    wiremock.verifyThat(0, getRequestedFor(urlPathEqualTo("/locations-units/libraries")));
    wiremock.verifyThat(1, postRequestedFor(urlPathMatching("/circulation-item/.{36}")));
    verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID);
    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
  }

  @Test
  @WireMockStub({
    "/stubs/mod-users/users/200-get-by-query(user).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode).json",
    "/stubs/mod-circulation/requests/201-post(any).json"
  })
  void createTransaction_positive_existingDcbItem() throws Exception {
    var patron = dcbPatron(EXISTED_PATRON_ID);
    postDcbTransaction(DCB_TRANSACTION_ID, borrowingPickupDcbTransaction(patron))
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(patron));

    wiremock.verifyThat(0, getRequestedFor(urlPathEqualTo("/locations")));
    wiremock.verifyThat(0, getRequestedFor(urlPathEqualTo("/locations-units/libraries")));
    wiremock.verifyThat(0, getRequestedFor(urlPathMatching("/circulation-item/.{36}")));
    verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID);
    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
  }

  @Test
  @WireMockStub("/stubs/mod-users/users/200-get-by-query(new_user empty).json")
  void createTransaction_positive_patronNotFound() throws Exception {
    var dcbPatron = dcbPatron(NOT_EXISTED_PATRON_ID);
    var dcbTransaction = borrowingPickupDcbTransaction(dcbPatron);

    postDcbTransactionAttempt(DCB_TRANSACTION_ID, dcbTransaction)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND_ERROR"))
      .andExpect(jsonPath("$.errors[0].message", is(format("Unable to find existing user "
        + "with barcode DCB_PATRON and id %s.", NOT_EXISTED_PATRON_ID))));

    var auditEntity = auditEntityVerifier.getLatestAuditEntity(DCB_TRANSACTION_ID);
    assertThat(auditEntity.getAction()).isEqualTo("ERROR");
  }

  @Test
  @WireMockStub({
    "/stubs/mod-users/users/200-get-by-query(patron).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
    "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
    "/stubs/mod-circulation-item/201-post(pickup).json",
    "/stubs/mod-circulation/requests/201-post(any).json"
  })
  void createTransaction_negative_attemptToCreateTransactionTwice() throws Exception {
    var dcbTransactionRequestBody = borrowingPickupDcbTransaction();
    postDcbTransaction(DCB_TRANSACTION_ID, dcbTransactionRequestBody)
      .andExpect(jsonPath("$.status").value("CREATED"));

    postDcbTransactionAttempt(DCB_TRANSACTION_ID, dcbTransactionRequestBody)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));

    verifyPostCirculationRequestCalledOnce(PATRON_TYPE_USER_ID);
    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
  }

  @Test
  @WireMockStub({
    "/stubs/mod-users/users/200-get-by-query(patron).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
    "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
    "/stubs/mod-circulation-item/201-post(pickup).json",
    "/stubs/mod-circulation/requests/201-post(any).json"
  })
  void createTransaction_positive_withPatronTypeUser() throws Exception {
    var transaction = borrowingPickupDcbTransaction();
    postDcbTransaction(DCB_TRANSACTION_ID, transaction)
      .andExpect(jsonPath("$.status").value("CREATED"));

    verifyPostCirculationRequestCalledOnce(PATRON_TYPE_USER_ID);
    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
  }

  @Test
  @Sql("/db/scripts/borrowing_pickup_transaction(created).sql")
  @WireMockStub("/stubs/mod-circulation/check-in-by-barcode/201-post(random SP).json")
  void updateTransactionStatus_positive_fromCreatedToOpen() throws Exception {
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(OPEN))
      .andExpect(jsonPath("$.status").value("OPEN"));
  }

  @Test
  @Sql("/db/scripts/borrowing_pickup_transaction(item_checked_in).sql")
  void updateTransactionStatus_positive_fromCheckedInToClosed() throws Exception {
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(CLOSED))
      .andExpect(jsonPath("$.status").value("CLOSED"));
  }

  @Test
  @Sql("/db/scripts/borrowing_pickup_transaction(item_checked_in).sql")
  void updateTransactionStatus_negative_fromCheckedInToCancelled() throws Exception {
    putDcbTransactionStatusAttempt(DCB_TRANSACTION_ID, transactionStatus(CANCELLED))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].message", startsWith("Cannot cancel transaction dcbTransactionId")))
      .andExpect(jsonPath("$.errors[0].code", is("VALIDATION_ERROR")));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-circulation/loans/200-get-by-query(dcb loans).json",
    "/stubs/mod-circulation/loans/204-put(max renewal).json"
  })
  @Sql("/db/scripts/borrowing_pickup_transaction(item_checked_out).sql")
  void blockRenewal_positive() throws Exception {
    mockMvc.perform(
        put("/transactions/{id}/block-renewal", DCB_TRANSACTION_ID)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var loanId = "d217d4d5-8b2b-496b-8aa5-7e60d530e124";
    wiremock.verifyThat(1, putRequestedFor(urlPathEqualTo("/circulation/loans/" + loanId))
      .withRequestBody(matchingJsonPath("$.renewalCount", equalTo("2147483647"))));
  }

  @Test
  @Sql("/db/scripts/borrowing_pickup_transaction(item_checked_in).sql")
  void blockRenewal_negative_invalidState() throws Exception {
    mockMvc.perform(
        put("/transactions/{id}/block-renewal", DCB_TRANSACTION_ID)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].code", is("VALIDATION_ERROR")))
      .andExpect(jsonPath("$.errors[0].message").value("DCB transaction has invalid state for "
        + "renewal block. Item must be already checked out by borrower or borrowing pickup role."));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-circulation/loans/200-get-by-query(dcb loans).json",
    "/stubs/mod-circulation/loans/204-put(min renewal).json"
  })
  @Sql("/db/scripts/borrowing_pickup_transaction(item_checked_out).sql")
  void unblockRenewal_positive() throws Exception {
    mockMvc.perform(put("/transactions/{id}/unblock-renewal", DCB_TRANSACTION_ID)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var loanId = "d217d4d5-8b2b-496b-8aa5-7e60d530e124";
    wiremock.verifyThat(1, putRequestedFor(urlPathEqualTo("/circulation/loans/" + loanId))
      .withRequestBody(matchingJsonPath("$.renewalCount", equalTo("0"))));
  }

  @Test
  @Sql("/db/scripts/borrowing_pickup_transaction(item_checked_in).sql")
  void unblockRenewal_negative_invalidState() throws Exception {
    mockMvc.perform(
        put("/transactions/{id}/unblock-renewal", DCB_TRANSACTION_ID)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].code", is("VALIDATION_ERROR")))
      .andExpect(jsonPath("$.errors[0].message").value("DCB transaction has invalid state for "
        + "renewal unblock. Item must be already checked out by borrower or borrowing pickup role."));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-users/users/200-get-by-query(patron).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode).json"
  })
  void createTransaction_negative_inventoryItemBarcodeConflict() throws Exception {
    postDcbTransactionAttempt(DCB_TRANSACTION_ID, borrowingPickupDcbTransaction())
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.errors[0].code").value("DUPLICATE_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value(containsString(
        "Unable to create item with barcode DCB_ITEM as it exists in inventory")));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(new barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(new barcode).json",
    "/stubs/mod-users/users/200-get-by-query(patron).json",
    "/stubs/mod-circulation-storage/request-storage/200-get-by-id(circ-item).json",
    "/stubs/mod-circulation/requests/201-post(any).json",
    "/stubs/mod-circulation/requests/204-put(any).json",
  })
  @Sql("/db/scripts/borrowing_pickup_transaction(created).sql")
  void updateTransaction_positive_newDcbItemProvidedAsReplacement() throws Exception {
    var updateTransaction = dcbTransactionUpdate();
    var newItemId = "27c1a543-c833-4669-8131-9dd344fc46ae";
    mockMvc.perform(
        put("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(updateTransaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(newItemId, PATRON_TYPE_USER_ID);

    wiremock.verifyThat(1, putRequestedFor(urlPathMatching("/circulation/requests/.{36}"))
      .withRequestBody(matchingJsonPath("$.requestType", equalTo("Hold")))
      .withRequestBody(matchingJsonPath("$.itemId", equalTo(ITEM_ID)))
      .withRequestBody(matchingJsonPath("$.status", equalTo("Closed - Cancelled")))
      .withRequestBody(matchingJsonPath("$.instanceId", equalTo(DCBConstants.INSTANCE_ID)))
      .withRequestBody(matchingJsonPath("$.requesterId", equalTo(PATRON_TYPE_USER_ID)))
      .withRequestBody(matchingJsonPath("$.pickupServicePointId", equalTo(BORROWER_SERVICE_POINT_ID)))
      .withRequestBody(matchingJsonPath("$.holdingsRecordId", equalTo(DCBConstants.HOLDING_ID))));
  }

  @Test
  @Sql("/db/scripts/borrowing_pickup_transaction(item_checked_out).sql")
  void updateTransaction_positive_invalidState() throws Exception {
    putDcbTransactionDetailsAttempt(DCB_TRANSACTION_ID, dcbTransactionUpdate())
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value("Transaction details should not be "
        + "updated from ITEM_CHECKED_OUT status, it can be updated only from CREATED status"));
  }

  private static void verifyPostCirculationRequestCalledOnce(String requesterId) {
    verifyPostCirculationRequestCalledOnce(ITEM_ID, requesterId);
  }

  private static void verifyPostCirculationRequestCalledOnce(String itemId, String requesterId) {
    wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/circulation/requests"))
      .withRequestBody(matchingJsonPath("$.requestType", equalTo("Hold")))
      .withRequestBody(matchingJsonPath("$.itemId", equalTo(itemId)))
      .withRequestBody(matchingJsonPath("$.instanceId", equalTo(DCBConstants.INSTANCE_ID)))
      .withRequestBody(matchingJsonPath("$.requesterId", equalTo(requesterId)))
      .withRequestBody(matchingJsonPath("$.pickupServicePointId", equalTo(PICKUP_SERVICE_POINT_ID)))
      .withRequestBody(matchingJsonPath("$.holdingsRecordId", equalTo(DCBConstants.HOLDING_ID))));
  }
}
