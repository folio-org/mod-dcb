package org.folio.dcb.it;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CANCELLED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.EXPIRED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_IN;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;
import static org.folio.dcb.utils.EntityUtils.BORROWER_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.ITEM_ID;
import static org.folio.dcb.utils.EntityUtils.NOT_EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.PATRON_GROUP_ID;
import static org.folio.dcb.utils.EntityUtils.PATRON_TYPE_USER_ID;
import static org.folio.dcb.utils.EntityUtils.PICKUP_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.dcbItem;
import static org.folio.dcb.utils.EntityUtils.dcbPatron;
import static org.folio.dcb.utils.EntityUtils.dcbTransactionUpdate;
import static org.folio.dcb.utils.EntityUtils.pickupDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.transactionStatus;
import static org.folio.dcb.utils.JsonTestUtils.asJsonString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.dcb.domain.dto.TransactionStatus.StatusEnum;
import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.folio.dcb.utils.DCBConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import support.types.IntegrationTest;
import support.wiremock.WireMockStub;

@IntegrationTest
class PickupTransactionIT extends BaseTenantIntegrationTest {

  @Test
  @WireMockStub({
    "/stubs/mod-users/users/200-get-by-query(new_user empty).json",
    "/stubs/mod-users/users/201-post-user(any).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-circulation-item/201-post(pickup).json",
    "/stubs/mod-circulation/requests/201-post(any).json",
  })
  void createTransaction_positive_newDcbItemAndUser() throws Exception {
    var patron = dcbPatron(NOT_EXISTED_PATRON_ID);
    var dcbTransaction = pickupDcbTransaction(patron);
    postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(patron));

    wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/users")));
    wiremock.verifyThat(1, postRequestedFor(urlPathMatching("/circulation-item/.{36}")));

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(NOT_EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub({
    "/stubs/mod-users/users/200-get-by-query(new_user empty).json",
    "/stubs/mod-users/users/201-post-user(any).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-circulation/requests/201-post(any).json",
  })
  void createTransaction_positive_newDcbUserWithLocalNames() throws Exception {
    var patron = dcbPatron(NOT_EXISTED_PATRON_ID, "[John, Doe]");
    var dcbTransaction = pickupDcbTransaction(patron);
    postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(patron));

    wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/users"))
      .withRequestBody(matchingJsonPath("$.personal.firstName", equalTo("John")))
      .withRequestBody(matchingJsonPath("$.personal.lastName", equalTo("Doe"))));

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(NOT_EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub({
    "/stubs/mod-users/users/200-get-by-query(user id+barcode).json",
    "/stubs/mod-users/users/204-put(any).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-circulation/requests/201-post(any).json",
  })
  void createTransaction_positive_userUpdateWithLocalNames() throws Exception {
    var patron = dcbPatron(EXISTED_PATRON_ID, "[John, Doe]");
    var dcbTransaction = pickupDcbTransaction(patron);
    postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(patron));

    wiremock.verifyThat(1, putRequestedFor(urlPathEqualTo("/users/" + EXISTED_PATRON_ID))
      .withRequestBody(matchingJsonPath("$.personal.lastName", equalTo("Doe")))
      .withRequestBody(matchingJsonPath("$.personal.firstName", equalTo("John"))));

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub({
    "/stubs/mod-users/users/200-get-by-query(new_user empty).json",
    "/stubs/mod-users/users/201-post-user(any).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-circulation/requests/201-post(any).json",
  })
  void createTransaction_positive_existingDcbItem() throws Exception {
    var patron = dcbPatron(NOT_EXISTED_PATRON_ID);
    var dcbTransaction = pickupDcbTransaction(patron);
    postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(patron));

    wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/users")));
    wiremock.verifyThat(0, postRequestedFor(urlPathMatching("/circulation-item/.{36}")));

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(NOT_EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub({
    "/stubs/mod-users/users/200-get-by-query(dcb user+no group).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-users/users/204-put(any).json",
    "/stubs/mod-circulation/requests/201-post(any).json",
  })
  void createTransaction_positive_dcbUserFoundWithoutGroup() throws Exception {
    var patron = dcbPatron(EXISTED_PATRON_ID);
    var dcbTransaction = pickupDcbTransaction(patron);
    postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(patron));

    wiremock.verifyThat(1, putRequestedFor(urlPathEqualTo("/users/" + EXISTED_PATRON_ID))
      .withRequestBody(matchingJsonPath("$.patronGroup", equalTo(PATRON_GROUP_ID))));
    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub("/stubs/mod-users/users/200-get-by-query(patron).json")
  void createTransaction_negative_userFoundByBarcode() throws Exception {
    var dcbPatron = dcbPatron(PATRON_TYPE_USER_ID);
    postDcbTransactionAttempt(DCB_TRANSACTION_ID, pickupDcbTransaction(dcbPatron))
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value(
        "User with type patron is retrieved. so unable to create transaction"));

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
  }

  @Test
  @WireMockStub({
    "/stubs/mod-users/users/200-get-by-query(dcb user).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
    "/stubs/mod-circulation-item/200-get-by-query(barcode).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-circulation/requests/201-post(any).json",
  })
  void createTransaction_positive_dcbUserFoundByBarcode() throws Exception {
    var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
    postDcbTransaction(DCB_TRANSACTION_ID, pickupDcbTransaction(dcbPatron))
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(dcbPatron));

    wiremock.verifyThat(0, postRequestedFor(urlPathEqualTo("/users")));
    wiremock.verifyThat(0, postRequestedFor(urlPathMatching("/circulation-item/.{36}")));

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub("/stubs/mod-circulation/check-in-by-barcode/201-post(random SP).json")
  void updateTransactionStatus_positive_fromCreatedToOpen() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, CREATED, pickupDcbTransaction());
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(OPEN))
      .andExpect(jsonPath("$.status").value("OPEN"));
  }

  @Test
  void updateTransactionStatus_negative_fromCheckedInToCancelled() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_IN, pickupDcbTransaction());
    putDcbTransactionStatusAttempt(DCB_TRANSACTION_ID, transactionStatus(CANCELLED))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code", is("VALIDATION_ERROR")))
      .andExpect(jsonPath("$.errors[0].message", containsString("Cannot cancel transaction")));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-users/users/200-get-by-query(user id+barcode).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode).json",
  })
  void createTransaction_negative_inventoryItemBarcodeConflict() throws Exception {
    postDcbTransactionAttempt(DCB_TRANSACTION_ID, pickupDcbTransaction())
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
  void updateTransaction_positive_newDcbItemProvidedAsReplacement() throws Exception {
    var dcbTransaction = pickupDcbTransaction(dcbPatron(PATRON_TYPE_USER_ID));
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
  void updateTransaction_positive_invalidState() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_IN, pickupDcbTransaction());

    putDcbTransactionDetailsAttempt(DCB_TRANSACTION_ID, dcbTransactionUpdate())
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value("Transaction details should not be "
        + "updated from ITEM_CHECKED_IN status, it can be updated only from CREATED status"));
  }

  @ParameterizedTest
  @EnumSource(value = StatusEnum.class, names = "EXPIRED", mode = EXCLUDE)
  void updateTransactionStatus_parameterized_invalidTransitionToExpiredStatus(
    StatusEnum sourceStatus) throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, sourceStatus, pickupDcbTransaction());

    putDcbTransactionStatusAttempt(DCB_TRANSACTION_ID, transactionStatus(EXPIRED))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value(containsString(String.format(
        "Status transition will not be possible from %s to EXPIRED", sourceStatus))));
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

