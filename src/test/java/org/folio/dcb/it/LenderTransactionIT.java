package org.folio.dcb.it;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.domain.dto.CirculationRequest.RequestTypeEnum.HOLD;
import static org.folio.dcb.domain.dto.CirculationRequest.RequestTypeEnum.PAGE;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.AWAITING_PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CANCELLED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.EXPIRED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_IN;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_OUT;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;
import static org.folio.dcb.utils.EntityUtils.BORROWER_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.HOLDING_RECORD_ID;
import static org.folio.dcb.utils.EntityUtils.INSTANCE_ID;
import static org.folio.dcb.utils.EntityUtils.ITEM_ID;
import static org.folio.dcb.utils.EntityUtils.NOT_EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.PATRON_TYPE_USER_ID;
import static org.folio.dcb.utils.EntityUtils.TEST_TENANT;
import static org.folio.dcb.utils.EntityUtils.dcbItem;
import static org.folio.dcb.utils.EntityUtils.dcbPatron;
import static org.folio.dcb.utils.EntityUtils.dcbTransactionUpdate;
import static org.folio.dcb.utils.EntityUtils.lenderDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.transactionStatus;
import static org.folio.dcb.utils.EventDataProvider.expiredRequestMessage;
import static org.folio.dcb.utils.EventDataProvider.itemCheckInMessage;
import static org.folio.dcb.utils.JsonTestUtils.asJsonString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import net.jpountz.util.UnsafeUtils;
import org.folio.dcb.domain.dto.TransactionStatus.StatusEnum;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.folio.dcb.support.types.IntegrationTest;
import org.folio.dcb.support.wiremock.WireMockStub;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@IntegrationTest
class LenderTransactionIT extends BaseTenantIntegrationTest {

  @Test
  @WireMockStub("/stubs/mod-circulation/requests/200-get-by-query(hold requests empty).json")
  void getTransactionStatus_positive_createdStatus() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, CREATED, lenderDcbTransaction());
    getDcbTransactionStatus(DCB_TRANSACTION_ID)
      .andExpect(content().json(asJsonString(new TransactionStatusResponse()
        .status(TransactionStatusResponse.StatusEnum.CREATED)
        .role(TransactionStatusResponse.RoleEnum.LENDER))))
      .andExpect(jsonPath("$.item.renewalInfo").doesNotExist())
      .andExpect(jsonPath("$.item.holdCount").value(0));
  }

  @Test
  @WireMockStub("/stubs/mod-circulation/requests/200-get-by-query(hold requests empty).json")
  void getTransactionStatus_positive_checkoutStatus() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, lenderDcbTransaction());
    getDcbTransactionStatus(DCB_TRANSACTION_ID)
      .andExpect(content().json(asJsonString(new TransactionStatusResponse()
        .status(TransactionStatusResponse.StatusEnum.ITEM_CHECKED_OUT)
        .role(TransactionStatusResponse.RoleEnum.LENDER))))
      .andExpect(jsonPath("$.item.renewalInfo").doesNotExist())
      .andExpect(jsonPath("$.item.holdCount").value(0));
  }

  @Test
  @WireMockStub("/stubs/mod-circulation/requests/200-get-by-query(hold requests).json")
  void getTransactionStatus_positive_awaitingPickupNoRenewalInfo() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, AWAITING_PICKUP, lenderDcbTransaction());
    getDcbTransactionStatus(DCB_TRANSACTION_ID)
      .andExpect(content().json(asJsonString(new TransactionStatusResponse()
        .status(TransactionStatusResponse.StatusEnum.AWAITING_PICKUP)
        .role(TransactionStatusResponse.RoleEnum.LENDER))))
      .andExpect(jsonPath("$.item.renewalInfo").doesNotExist())
      .andExpect(jsonPath("$.item.holdCount").value(3));
  }

  @Test
  void getTransactionStatus_negative_transactionNotFound() throws Exception {
    getDcbTransactionStatusAttempt(DCB_TRANSACTION_ID)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.errors[0].message", startsWith("DCB Transaction was not found")))
      .andExpect(jsonPath("$.errors[0].code", startsWith("NOT_FOUND_ERROR")));
  }

  @Test
  @WireMockStub(value = {
    "/stubs/mod-users/users/200-get-by-query(user id+barcode).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode).json",
    "/stubs/mod-inventory-storage/holdings-storage/200-get-by-id.json",
    "/stubs/mod-circulation/requests/201-post(any).json"
  })
  void createTransaction_positive_availableItem() throws Exception {
    postDcbTransaction(DCB_TRANSACTION_ID, lenderDcbTransaction())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(dcbPatron(EXISTED_PATRON_ID)))
      .andExpect(jsonPath("$.item.locationCode").doesNotExist());

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(PAGE.getValue(), EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub(value = {
    "/stubs/mod-users/users/200-get-by-query(user id+barcode).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode+in-transit).json",
    "/stubs/mod-inventory-storage/holdings-storage/200-get-by-id.json",
    "/stubs/mod-circulation/requests/201-post(any).json"
  })
  void createTransaction_positive_inTransitItem() throws Exception {
    postDcbTransaction(DCB_TRANSACTION_ID, lenderDcbTransaction())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(dcbPatron(EXISTED_PATRON_ID)))
      .andExpect(jsonPath("$.item.locationCode").doesNotExist());

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(HOLD.getValue(), EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub(value = {
    "/stubs/mod-users/users/200-get-by-query(user id+barcode).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode).json",
    "/stubs/mod-inventory-storage/holdings-storage/200-get-by-id.json",
    "/stubs/mod-circulation/requests/201-post(any).json"
  })
  void createTransaction_positive_itemWithLocationCode() throws Exception {
    var dcbItem = dcbItem().locationCode("TEST_ITEM_LOCATION_CODE");
    var dcbTransaction = lenderDcbTransaction().item(dcbItem);
    postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem))
      .andExpect(jsonPath("$.patron").value(dcbPatron(EXISTED_PATRON_ID)))
      .andExpect(jsonPath("$.item.locationCode").exists());

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(PAGE.getValue(), EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub(value = {
    "/stubs/mod-users/users/200-get-by-query(user id+barcode).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode).json",
    "/stubs/mod-inventory-storage/holdings-storage/200-get-by-id.json",
    "/stubs/mod-circulation/requests/201-post(any).json"
  })
  void createTransaction_negative_attemptToCreateTransactionTwice() throws Exception {
    var dcbTransactionRequestBody = lenderDcbTransaction();
    postDcbTransaction(DCB_TRANSACTION_ID, dcbTransactionRequestBody)
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(dcbPatron(EXISTED_PATRON_ID)));

    postDcbTransactionAttempt(DCB_TRANSACTION_ID, dcbTransactionRequestBody)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(PAGE.getValue(), EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub(value = {
    "/stubs/mod-users/users/200-get-by-query(new_user empty).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-users/users/201-post-user(any).json",
    "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode).json",
    "/stubs/mod-inventory-storage/holdings-storage/200-get-by-id.json",
    "/stubs/mod-circulation/requests/201-post(any).json"
  })
  void createTransaction_positive_newDcbPatron() throws Exception {
    var patron = dcbPatron(NOT_EXISTED_PATRON_ID);

    postDcbTransaction(DCB_TRANSACTION_ID, lenderDcbTransaction(patron))
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(patron));

    wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/users"))
      .withRequestBody(matchingJsonPath("$.personal.lastName", equalTo("DcbSystem"))));
    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(PAGE.getValue(), NOT_EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub(value = {
    "/stubs/mod-users/users/200-get-by-query(new_user empty).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-users/users/201-post-user(any).json",
    "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode).json",
    "/stubs/mod-inventory-storage/holdings-storage/200-get-by-id.json",
    "/stubs/mod-circulation/requests/201-post(any).json"
  })
  void createTransaction_positive_newDcbPatronWithLocalNames() throws Exception {
    var patron = dcbPatron(NOT_EXISTED_PATRON_ID, "[John, Doe]");

    postDcbTransaction(DCB_TRANSACTION_ID, lenderDcbTransaction(patron))
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(patron));

    wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/users"))
      .withRequestBody(matchingJsonPath("$.personal.lastName", equalTo("Doe")))
      .withRequestBody(matchingJsonPath("$.personal.firstName", equalTo("John"))));
    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(PAGE.getValue(), NOT_EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub(value = {
    "/stubs/mod-users/users/200-get-by-query(user id+barcode).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-users/users/204-put(any).json",
    "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode).json",
    "/stubs/mod-inventory-storage/holdings-storage/200-get-by-id.json",
    "/stubs/mod-circulation/requests/201-post(any).json"
  })
  void createTransaction_positive_dcbUserUpdateWithLocalNames() throws Exception {
    var patron = dcbPatron(EXISTED_PATRON_ID, "[John, Doe]");

    postDcbTransaction(DCB_TRANSACTION_ID, lenderDcbTransaction(patron))
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem()))
      .andExpect(jsonPath("$.patron").value(patron));

    wiremock.verifyThat(1, putRequestedFor(urlPathEqualTo("/users/" + EXISTED_PATRON_ID))
      .withRequestBody(matchingJsonPath("$.personal.lastName", equalTo("Doe")))
      .withRequestBody(matchingJsonPath("$.personal.firstName", equalTo("John"))));
    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(PAGE.getValue(), EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub(value = {
    "/stubs/mod-users/users/200-get-by-query(user id+barcode).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode empty).json"
  })
  void createTransaction_negative_itemNotFoundByQuery() throws Exception {
    var dcbTransaction = lenderDcbTransaction();

    postDcbTransactionAttempt(DCB_TRANSACTION_ID, dcbTransaction)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value("Unable to find existing item with "
        + "id 5b95877d-86c0-4cb7-a0cd-7660b348ae5a and barcode DCB_ITEM."));

    var latestAuditEntity = auditEntityVerifier.getLatestAuditEntity(DCB_TRANSACTION_ID);
    assertThat(latestAuditEntity.getAction()).isEqualTo("ERROR");
  }

  @Test
  @WireMockStub(value = {
    "/stubs/mod-users/users/200-get-by-query(user id+barcode).json",
    "/stubs/mod-users/groups/200-get-by-query(staff).json",
    "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
    "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
    "/stubs/mod-calendar/calendars/200-get-all.json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode-special chars).json",
    "/stubs/mod-inventory-storage/holdings-storage/200-get-by-id.json",
    "/stubs/mod-circulation/requests/201-post(any).json"
  })
  void createTransaction_positive_itemWithSpecialCharsInBarcode() throws Exception {
    var specialCharBarCode = "!@#$%^^&&*()_=+`~|\\]{}DCB_ITEM/'''";
    var dcbItem = dcbItem().barcode(specialCharBarCode);
    var dcbTransaction = lenderDcbTransaction().item(dcbItem);

    postDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction)
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(dcbItem))
      .andExpect(jsonPath("$.patron").value(dcbPatron()));

    auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    verifyPostCirculationRequestCalledOnce(PAGE.getValue(), EXISTED_PATRON_ID);
  }

  @Test
  @WireMockStub("/stubs/mod-users/users/200-get-by-query(patron).json")
  void createTransaction_positive_foundUserTypeIsPatron() throws Exception {
    var transaction = lenderDcbTransaction().patron(dcbPatron(PATRON_TYPE_USER_ID));
    postDcbTransactionAttempt(DCB_TRANSACTION_ID, transaction)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value(
        "User with type patron is retrieved. so unable to create transaction"));
  }

  @Test
  @WireMockStub("/stubs/mod-circulation/check-in-by-barcode/201-post(dcb+pickup_sp).json")
  void updateTransactionStatus_positive_fromOpenToAwaitingPickup() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, OPEN, lenderDcbTransaction());
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(AWAITING_PICKUP))
      .andExpect(jsonPath("$.status").value("AWAITING_PICKUP"));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-circulation/check-in-by-barcode/201-post(dcb+pickup_sp).json",
    "/stubs/mod-circulation/check-out-by-barcode/201-post(dcb+pickup_sp).json",
  })
  void updateTransactionStatus_positive_fromOpenToCheckedIn() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, OPEN, lenderDcbTransaction());
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(ITEM_CHECKED_IN))
      .andExpect(jsonPath("$.status").value("ITEM_CHECKED_IN"));
  }

  @Test
  @WireMockStub("/stubs/mod-circulation/check-out-by-barcode/201-post(dcb+pickup_sp).json")
  void updateTransactionStatus_positive_fromAwaitingPickupToCheckedOut() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, AWAITING_PICKUP, lenderDcbTransaction());
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(ITEM_CHECKED_OUT))
      .andExpect(jsonPath("$.status").value("ITEM_CHECKED_OUT"));
  }

  @Test
  void updateTransactionStatus_positive_fromCheckedOutToCheckedIn() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, lenderDcbTransaction());
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(ITEM_CHECKED_IN))
      .andExpect(jsonPath("$.status").value("ITEM_CHECKED_IN"));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-circulation-storage/request-storage/200-get-by-id(item).json",
    "/stubs/mod-circulation/requests/204-put(any).json"
  })
  void updateTransactionStatus_positive_fromCreatedToCancelled() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, CREATED, lenderDcbTransaction());
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(CANCELLED))
      .andExpect(jsonPath("$.status").value("CANCELLED"));

    wiremock.verifyThat(1, putRequestedFor(urlPathMatching("/circulation/requests/.{36}"))
      .withRequestBody(matchingJsonPath("$.requestType", equalTo("Page")))
      .withRequestBody(matchingJsonPath("$.itemId", equalTo(ITEM_ID)))
      .withRequestBody(matchingJsonPath("$.status", equalTo("Closed - Cancelled")))
      .withRequestBody(matchingJsonPath("$.instanceId", equalTo(INSTANCE_ID)))
      .withRequestBody(matchingJsonPath("$.requesterId", equalTo(EXISTED_PATRON_ID)))
      .withRequestBody(matchingJsonPath("$.pickupServicePointId", equalTo(BORROWER_SERVICE_POINT_ID)))
      .withRequestBody(matchingJsonPath("$.holdingsRecordId", equalTo(HOLDING_RECORD_ID))));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-circulation-storage/request-storage/200-get-by-id(item).json",
    "/stubs/mod-circulation/requests/204-put(any).json"
  })
  void updateTransactionStatus_positive_fromAwaitingPickupToCancelled() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, AWAITING_PICKUP, lenderDcbTransaction());
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(CANCELLED))
      .andExpect(jsonPath("$.status").value("CANCELLED"));

    wiremock.verifyThat(1, putRequestedFor(urlPathMatching("/circulation/requests/.{36}"))
      .withRequestBody(matchingJsonPath("$.requestType", equalTo("Page")))
      .withRequestBody(matchingJsonPath("$.itemId", equalTo(ITEM_ID)))
      .withRequestBody(matchingJsonPath("$.status", equalTo("Closed - Cancelled")))
      .withRequestBody(matchingJsonPath("$.instanceId", equalTo(INSTANCE_ID)))
      .withRequestBody(matchingJsonPath("$.requesterId", equalTo(EXISTED_PATRON_ID)))
      .withRequestBody(matchingJsonPath("$.pickupServicePointId", equalTo(BORROWER_SERVICE_POINT_ID)))
      .withRequestBody(matchingJsonPath("$.holdingsRecordId", equalTo(HOLDING_RECORD_ID))));
  }

  @Test
  void updateTransactionStatus_negative_fromItemCheckedInToCancelled() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_IN, lenderDcbTransaction());
    putDcbTransactionStatusAttempt(DCB_TRANSACTION_ID, transactionStatus(CANCELLED))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].message").value(startsWith("Cannot cancel transaction dcbTransactionId")))
      .andExpect(jsonPath("$.errors[0].message").value(
        containsString("Transaction already in status: ITEM_CHECKED_IN")));
  }

  @ParameterizedTest
  @EnumSource(value = StatusEnum.class, names = "EXPIRED", mode = EXCLUDE)
  void updateTransactionStatus_parameterized_invalidTransitionToExpiredStatus(
    StatusEnum sourceStatus) throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, sourceStatus, lenderDcbTransaction());

    putDcbTransactionStatusAttempt(DCB_TRANSACTION_ID, transactionStatus(EXPIRED))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value(containsString(String.format(
        "Status transition will not be possible from %s to EXPIRED", sourceStatus))));
  }

  @ParameterizedTest
  @EnumSource(value = StatusEnum.class, names = "EXPIRED", mode = EXCLUDE)
  void updateTransactionStatus_parameterized_invalidTransitionFromExpiredStatus(
    StatusEnum targetStatus) throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, EXPIRED, lenderDcbTransaction());

    putDcbTransactionStatusAttempt(DCB_TRANSACTION_ID, transactionStatus(targetStatus))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value(containsString(String.format(
        "Status transition will not be possible from EXPIRED to %s", targetStatus))));
  }

  @Test
  void createTransaction_negative_invalidItemId() throws Exception {
    var dcbTransaction = lenderDcbTransaction().item(dcbItem().id("1234"));
    postDcbTransactionAttempt(DCB_TRANSACTION_ID, dcbTransaction)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));

    auditEntityVerifier.assertEmpty();
  }

  @Test
  void updateTransactionStatus_positive_fromCreatedToOpen() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, CREATED, lenderDcbTransaction());
    putDcbTransactionStatus(DCB_TRANSACTION_ID, transactionStatus(OPEN))
      .andExpect(jsonPath("$.status").value("OPEN"));
  }

  @Test
  void updateTransaction_negative_invalidRole() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, CREATED, lenderDcbTransaction());
    putDcbTransactionDetailsAttempt(DCB_TRANSACTION_ID, dcbTransactionUpdate())
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value("Item details cannot be updated for lender role"));
  }

  @Test
  void updateTransaction_negativeParameterized_invalidState() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_IN, lenderDcbTransaction());
    putDcbTransactionDetailsAttempt(DCB_TRANSACTION_ID, dcbTransactionUpdate())
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value("Transaction details should not be "
        + "updated from ITEM_CHECKED_IN status, it can be updated only from CREATED status"));
  }

  @Test
  @WireMockStub("/stubs/mod-circulation/requests/200-get-by-query(hold requests empty).json")
  void updateStatus_positive_awaitingPickupTransactionExpiration() {
    var transactionId = UUID.randomUUID().toString();
    testJdbcHelper.saveDcbTransaction(transactionId, AWAITING_PICKUP, lenderDcbTransaction());
    testEventHelper.sendMessage(expiredRequestMessage(TEST_TENANT));

    awaitUntilAsserted(() -> getDcbTransactionStatus(transactionId)
      .andExpect(jsonPath("$.status").value(EXPIRED.getValue())));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-circulation/requests/200-get-by-query(hold requests empty).json",
    "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+available).json",
  })
  void updateStatus_positive_expiredToClosedTransitionAfterCheckInMessage() {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, EXPIRED, lenderDcbTransaction());
    testEventHelper.sendMessage(itemCheckInMessage(TEST_TENANT));

    awaitUntilAsserted(() -> getDcbTransactionStatus(DCB_TRANSACTION_ID)
      .andExpect(jsonPath("$.status").value(CLOSED.getValue())));
  }

  private static void verifyPostCirculationRequestCalledOnce(String type, String requesterId) {
    wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/circulation/requests"))
      .withRequestBody(matchingJsonPath("$.requestType", equalTo(type)))
      .withRequestBody(matchingJsonPath("$.itemId", equalTo(ITEM_ID)))
      .withRequestBody(matchingJsonPath("$.instanceId", equalTo(INSTANCE_ID)))
      .withRequestBody(matchingJsonPath("$.requesterId", equalTo(requesterId)))
      .withRequestBody(matchingJsonPath("$.pickupServicePointId", equalTo(BORROWER_SERVICE_POINT_ID)))
      .withRequestBody(matchingJsonPath("$.holdingsRecordId", equalTo(HOLDING_RECORD_ID))));
  }
}

