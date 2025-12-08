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
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.AWAITING_PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CANCELLED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_IN;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_OUT;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;
import static org.folio.dcb.utils.EntityUtils.BORROWER_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.ITEM_ID;
import static org.folio.dcb.utils.EntityUtils.LOAN_ID;
import static org.folio.dcb.utils.EntityUtils.NOT_EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.PATRON_TYPE_USER_ID;
import static org.folio.dcb.utils.EntityUtils.borrowerDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.dcbItem;
import static org.folio.dcb.utils.EntityUtils.dcbPatron;
import static org.folio.dcb.utils.EntityUtils.dcbTransactionUpdate;
import static org.folio.dcb.utils.EntityUtils.transactionStatus;
import static org.folio.dcb.utils.JsonTestUtils.asJsonString;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.folio.dcb.support.types.IntegrationTest;
import org.folio.dcb.support.wiremock.WireMockStub;
import org.folio.dcb.utils.DCBConstants;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@IntegrationTest
class BorrowerTransactionIT extends BaseTenantIntegrationTest {

  @Test
  void getTransactionStatus_positive_createdStatus() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, CREATED, borrowerDcbTransaction());
    getDcbTransactionStatus(DCB_TRANSACTION_ID)
      .andExpect(jsonPath("$.item").doesNotExist())
      .andExpect(jsonPath("$.status").value("CREATED"));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-circulation/loans/200-get-by-query(dcb loans).json",
    "/stubs/mod-circulation-storage/loan-policy-storage/200-get-by-query(limited renewals).json",
  })
  void getTransactionStatus_positive_itemCheckedOutStatus() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, borrowerDcbTransaction());
    getDcbTransactionStatus(DCB_TRANSACTION_ID)
      .andExpect(jsonPath("$.item.renewalInfo.renewalCount").value(8))
      .andExpect(jsonPath("$.item.renewalInfo.renewable").value(true))
      .andExpect(jsonPath("$.item.renewalInfo.renewalMaxCount").value(22));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-inventory-storage/service-points/200-get-by-query(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-users/users/200-get-by-query(user).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
    "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
    "/stubs/mod-circulation-item/201-post(borrower).json",
    "/stubs/mod-circulation/requests/201-post(any).json",
  })
  void createTransaction_positive_newDcbItem() throws Exception {
    var patron = dcbPatron(EXISTED_PATRON_ID);
    var dcbTransaction = borrowerDcbTransaction(patron);
    postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(patron));

    wiremock.verifyThat(0, getRequestedFor(urlPathEqualTo("/locations")));
    wiremock.verifyThat(0, getRequestedFor(urlPathEqualTo("/locations-units/libraries")));
    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub({
    "/stubs/mod-inventory-storage/service-points/200-get-by-query(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-users/users/200-get-by-query(patron).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode).json",
    "/stubs/mod-circulation/requests/201-post(any).json",
  })
  void createTransaction_positive_dcbItemExists() throws Exception {
    postDcbTransaction(DCB_TRANSACTION_ID, borrowerDcbTransaction())
      .andExpect(jsonPath("$.status").value("CREATED"));

    wiremock.verifyThat(0, getRequestedFor(urlPathEqualTo("/locations")));
    wiremock.verifyThat(0, getRequestedFor(urlPathEqualTo("/locations-units/libraries")));
    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(PATRON_TYPE_USER_ID);
  }

  @Test
  @WireMockStub({
    "/stubs/mod-inventory-storage/service-points/200-get-by-query(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-users/users/200-get-by-query(new_user empty).json",
  })
  void createTransaction_positive_patronNotFound() throws Exception {
    var dcbPatron = dcbPatron(NOT_EXISTED_PATRON_ID);
    var dcbTransaction = borrowerDcbTransaction(dcbPatron);

    postDcbTransactionAttempt(DCB_TRANSACTION_ID, dcbTransaction)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].code", is("NOT_FOUND_ERROR")))
      .andExpect(jsonPath("$.errors[0].message", is(
        format("Unable to find existing user with barcode DCB_PATRON and id %s.", NOT_EXISTED_PATRON_ID))));

    var auditEntity = auditEntityVerifier.getLatestAuditEntity(DCB_TRANSACTION_ID);
    assertThat(auditEntity.getAction()).isEqualTo("ERROR");
  }

  @Test
  @WireMockStub({
    "/stubs/mod-inventory-storage/service-points/200-get-by-query(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-users/users/200-get-by-query(patron).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode).json",
    "/stubs/mod-circulation/requests/201-post(any).json",
  })
  void createTransaction_negative_attemptToCreateTransactionTwice() throws Exception {
    var dcbTransactionRequestBody = borrowerDcbTransaction();
    postDcbTransaction(DCB_TRANSACTION_ID, dcbTransactionRequestBody)
      .andExpect(jsonPath("$.status").value("CREATED"));

    postDcbTransactionAttempt(DCB_TRANSACTION_ID, dcbTransactionRequestBody)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(PATRON_TYPE_USER_ID);
  }

  @Test
  @WireMockStub({
    "/stubs/mod-inventory-storage/service-points/200-get-by-query(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-users/users/200-get-by-query(patron).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode).json",
    "/stubs/mod-circulation/requests/201-post(any).json",
  })
  void createTransaction_positive_withPatronTypeUser() throws Exception {
    var transaction = borrowerDcbTransaction();
    postDcbTransaction(DCB_TRANSACTION_ID, transaction)
      .andExpect(jsonPath("$.status").value("CREATED"));

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(PATRON_TYPE_USER_ID);
  }

  @Test
  @WireMockStub("/stubs/mod-circulation/check-out-by-barcode/201-post(dcb+borrower_sp).json")
  void updateTransactionStatus_positive_fromAwaitingPickupToItemCheckedOut() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, AWAITING_PICKUP, borrowerDcbTransaction());
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(ITEM_CHECKED_OUT))
      .andExpect(jsonPath("$.status").value("ITEM_CHECKED_OUT"));
  }

  @Test
  @WireMockStub("/stubs/mod-circulation/check-in-by-barcode/201-post(random SP).json")
  void updateTransactionStatus_positive_fromItemCheckedOutToItemCheckedIn() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, borrowerDcbTransaction());
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(ITEM_CHECKED_IN))
      .andExpect(jsonPath("$.status").value("ITEM_CHECKED_IN"));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-circulation/check-in-by-barcode/201-post(random SP).json",
    "/stubs/mod-circulation/check-out-by-barcode/201-post(dcb+borrower_sp).json",
  })
  void updateTransactionStatus_positive_fromCreatedToClosed() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, CREATED, borrowerDcbTransaction());
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(CLOSED))
      .andExpect(jsonPath("$.status").value("CLOSED"));
  }

  @Test
  @WireMockStub("/stubs/mod-circulation/check-in-by-barcode/201-post(random SP).json")
  void updateTransactionStatus_positive_fromOpenToAwaitingPickup() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, OPEN, borrowerDcbTransaction());
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(AWAITING_PICKUP))
      .andExpect(jsonPath("$.status").value("AWAITING_PICKUP"));
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
  void updateTransaction_positive_newDcbItemProvidedAsReplacement() throws Exception {
    var dcbTransaction = borrowerDcbTransaction(dcbPatron(PATRON_TYPE_USER_ID));
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, CREATED, dcbTransaction);

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
  @WireMockStub({
    "/stubs/mod-circulation/loans/200-get-by-query(dcb loans).json",
    "/stubs/mod-circulation/loans/204-put(max renewal).json"
  })
  void blockRenewal_positive() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, borrowerDcbTransaction());
    mockMvc.perform(
        put("/transactions/{id}/block-renewal", DCB_TRANSACTION_ID)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());

    wiremock.verifyThat(1, putRequestedFor(urlPathEqualTo("/circulation/loans/" + LOAN_ID))
      .withRequestBody(matchingJsonPath("$.renewalCount", equalTo("2147483647"))));
  }

  @Test
  void blockRenewal_negative_invalidState() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, OPEN, borrowerDcbTransaction());
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
    "/stubs/mod-circulation/loans/204-put(min renewal).json",
  })
  void unblockRenewal_positive() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, borrowerDcbTransaction());
    mockMvc.perform(put("/transactions/{id}/unblock-renewal", DCB_TRANSACTION_ID)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());

    wiremock.verifyThat(1, putRequestedFor(urlPathEqualTo("/circulation/loans/" + LOAN_ID))
      .withRequestBody(matchingJsonPath("$.renewalCount", equalTo("0"))));
  }

  @Test
  void unblockRenewal_negative_invalidState() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, OPEN, borrowerDcbTransaction());
    mockMvc.perform(put("/transactions/{id}/unblock-renewal", DCB_TRANSACTION_ID)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].code", is("VALIDATION_ERROR")))
      .andExpect(jsonPath("$.errors[0].message").value("DCB transaction has invalid state for "
        + "renewal unblock. Item must be already checked out by borrower or borrowing pickup role."));
  }

  @Test
  void updateTransactionStatus_negative_fromClosedToCancelled() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, CLOSED, borrowerDcbTransaction());
    putDcbTransactionStatusAttempt(DCB_TRANSACTION_ID, transactionStatus(CANCELLED))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code", is("VALIDATION_ERROR")))
      .andExpect(jsonPath("$.errors[0].message", containsString("Cannot cancel transaction")));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-inventory-storage/service-points/200-get-by-query(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-users/users/200-get-by-query(patron).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode).json",
  })
  void createTransaction_negative_inventoryItemBarcodeConflict() throws Exception {
    postDcbTransactionAttempt(DCB_TRANSACTION_ID, borrowerDcbTransaction())
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.errors[0].code").value("DUPLICATE_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value(containsString(
        "Unable to create item with barcode DCB_ITEM as it exists in inventory")));

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
  }

  @Test
  void updateTransaction_positive_invalidState() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, borrowerDcbTransaction());
    putDcbTransactionDetailsAttempt(DCB_TRANSACTION_ID, dcbTransactionUpdate())
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value("Transaction details should not be "
        + "updated from ITEM_CHECKED_OUT status, it can be updated only from CREATED status"));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-inventory-storage/service-points/200-get-by-query(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-users/users/200-get-by-query(patron).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
    "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
    "/stubs/mod-circulation-item/201-post(borrower).json",
    "/stubs/mod-circulation/requests/201-post(any).json",
    "/stubs/mod-circulation/check-in-by-barcode/201-post(random SP).json",
    "/stubs/mod-circulation/check-out-by-barcode/201-post(dcb+borrower_sp).json",
  })
  void transactionStatusUpdate_positive_fullFlow() throws Exception {
    var startDate1 = OffsetDateTime.now(ZoneOffset.UTC);
    postDcbTransaction(DCB_TRANSACTION_ID, borrowerDcbTransaction())
      .andExpect(jsonPath("$.status").value("CREATED"));

    verifyPostCirculationRequestCalledOnce(PATRON_TYPE_USER_ID);

    // Update transaction from CREATED to OPEN
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(OPEN))
      .andExpect(jsonPath("$.status").value("OPEN"));

    // There is one update happened(CREATED -> OPEN), so we will get one record
    var endDate1 = OffsetDateTime.now(ZoneOffset.UTC);
    getDcbTransactionStatuses(startDate1, endDate1)
      .andExpect(jsonPath("$.totalRecords").value(1))
      .andExpect(jsonPath("$.currentPageNumber").value(0))
      .andExpect(jsonPath("$.currentPageSize").value(1000))
      .andExpect(jsonPath("$.maximumPageNumber").value(0))
      .andExpect(jsonPath("$.transactions[0].status").value("OPEN"))
      .andExpect(jsonPath("$.transactions[0].id").value(DCB_TRANSACTION_ID));

    var startDate2 = OffsetDateTime.now(ZoneOffset.UTC);

    // Update transaction from OPEN to CLOSED
    // There are 4 update happened(OPEN -> AWAITING_PICKUP, AWAITING_PICKUP -> ITEM_CHECKED_OUT
    // ITEM_CHECKED_OUT -> ITEM_CHECKED_IN, ITEM_CHECKED_IN -> CLOSED), so we will get 4 record
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(CLOSED))
      .andExpect(jsonPath("$.status").value("CLOSED"));

    var endDate2 = OffsetDateTime.now(ZoneOffset.UTC);
    getDcbTransactionStatuses(startDate2, endDate2)
      .andExpect(jsonPath("$.totalRecords", is(4)))
      .andExpect(jsonPath("$.currentPageNumber", is(0)))
      .andExpect(jsonPath("$.currentPageSize", is(1000)))
      .andExpect(jsonPath("$.maximumPageNumber", is(0)))
      .andExpect(jsonPath("$.transactions[*].status").value(
        containsInRelativeOrder("AWAITING_PICKUP", "ITEM_CHECKED_OUT", "ITEM_CHECKED_IN", "CLOSED")));

    // Now try to get all the records from startDate1 to endDate2 without pageSize(default pageSize)
    getDcbTransactionStatuses(startDate1, endDate2)
      .andExpect(jsonPath("$.totalRecords", is(5)))
      .andExpect(jsonPath("$.currentPageNumber", is(0)))
      .andExpect(jsonPath("$.currentPageSize", is(1000)))
      .andExpect(jsonPath("$.maximumPageNumber", is(0)))
      .andExpect(jsonPath("$.transactions[*].status").value(containsInRelativeOrder(
        "OPEN", "AWAITING_PICKUP", "ITEM_CHECKED_OUT", "ITEM_CHECKED_IN", "CLOSED")));

    // Now try to get the records based on pagination from startDate1 to endDate2.
    var queryParameters = Map.of("fromDate", startDate1, "toDate", endDate2, "pageNumber", 1, "pageSize", 2);
    getDcbTransactionStatuses(queryParameters)
      .andExpect(jsonPath("$.totalRecords", is(5)))
      .andExpect(jsonPath("$.currentPageNumber", is(1)))
      .andExpect(jsonPath("$.currentPageSize", is(2)))
      .andExpect(jsonPath("$.maximumPageNumber", is(2)))
      .andExpect(jsonPath("$.transactions[*].status",
        containsInRelativeOrder("ITEM_CHECKED_OUT", "ITEM_CHECKED_IN")));
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
      .withRequestBody(matchingJsonPath("$.pickupServicePointId", equalTo(BORROWER_SERVICE_POINT_ID)))
      .withRequestBody(matchingJsonPath("$.holdingsRecordId", equalTo(DCBConstants.HOLDING_ID))));
  }
}
