package org.folio.dcb.it;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.folio.dcb.domain.dto.CirculationRequest.RequestTypeEnum.HOLD;
import static org.folio.dcb.domain.dto.CirculationRequest.RequestTypeEnum.PAGE;
import static org.folio.dcb.utils.EntityUtils.BORROWER_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.HOLDING_RECORD_ID;
import static org.folio.dcb.utils.EntityUtils.INSTANCE_ID;
import static org.folio.dcb.utils.EntityUtils.ITEM_ID;
import static org.folio.dcb.utils.EntityUtils.NOT_EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.PATRON_TYPE_USER_ID;
import static org.folio.dcb.utils.EntityUtils.PICKUP_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.borrowerDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.borrowingPickupDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.dcbPatron;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import support.types.IntegrationTest;
import support.wiremock.WireMockStub;

@IntegrationTest
class SelfBorrowingTransactionIT extends BaseTenantIntegrationTest {

  @Nested
  @DisplayName("BorrowerRoleIT")
  class BorrowerRoleIT {

    @Test
    @WireMockStub({
      "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
      "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
      "/stubs/mod-calendar/calendars/200-get-all.json",
      "/stubs/mod-users/users/200-get-by-query(patron).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode).json",
      "/stubs/mod-inventory-storage/holdings-storage/200-get-by-id.json",
      "/stubs/mod-circulation/requests/201-post(any).json"
    })
    void createTransaction_positive_selfBorrowingAvailableItem() throws Exception {
      var selfBorrowingTransaction = borrowerDcbTransaction(true);

      postDcbTransaction(DCB_TRANSACTION_ID, selfBorrowingTransaction)
        .andExpect(jsonPath("$.status").value("CREATED"));

      verifyPostCirculationRequestCalledOnce(PAGE.getValue(), BORROWER_SERVICE_POINT_ID);
      auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
      "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
      "/stubs/mod-calendar/calendars/200-get-all.json",
      "/stubs/mod-users/users/200-get-by-query(patron).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode+in-transit).json",
      "/stubs/mod-inventory-storage/holdings-storage/200-get-by-id.json",
      "/stubs/mod-circulation/requests/201-post(any).json"
    })
    void createTransaction_positive_selfBorrowingPageRequestItemInTransit() throws Exception {
      var selfBorrowingTransaction = borrowerDcbTransaction(true);

      postDcbTransaction(DCB_TRANSACTION_ID, selfBorrowingTransaction)
        .andExpect(jsonPath("$.status").value("CREATED"));

      verifyPostCirculationRequestCalledOnce(HOLD.getValue(), BORROWER_SERVICE_POINT_ID);
      auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
      "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
      "/stubs/mod-calendar/calendars/200-get-all.json",
      "/stubs/mod-users/users/200-get-by-query(new_user empty).json"
    })
    void createTransaction_negative_selfBorrowingPatronNotFound() throws Exception {
      var dcbPatron = dcbPatron(NOT_EXISTED_PATRON_ID);
      var selfBorrowingTransaction = borrowerDcbTransaction(dcbPatron, true);

      postDcbTransactionAttempt(DCB_TRANSACTION_ID, selfBorrowingTransaction)
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND_ERROR"))
        .andExpect(jsonPath("$.errors[0].message").value(("Unable to find existing user with"
          + " barcode DCB_PATRON and id %s.").formatted(NOT_EXISTED_PATRON_ID)));

      var auditEntity = auditEntityVerifier.getLatestAuditEntity(DCB_TRANSACTION_ID);
      assertThat(auditEntity.getAction()).isEqualTo("ERROR");
    }

    @Test
    @WireMockStub({
      "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
      "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
      "/stubs/mod-calendar/calendars/200-get-all.json",
      "/stubs/mod-users/users/200-get-by-query(patron).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode empty).json"
    })
    void createTransaction_negative_selfBorrowingItemNotFound() throws Exception {
      var selfBorrowingTransaction = borrowerDcbTransaction(true);

      postDcbTransactionAttempt(DCB_TRANSACTION_ID, selfBorrowingTransaction)
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND_ERROR"))
        .andExpect(jsonPath("$.errors[0].message").value(("Unable to find existing "
          + "item with id %s and barcode DCB_ITEM.").formatted(ITEM_ID)));

      var auditEntity = auditEntityVerifier.getLatestAuditEntity(DCB_TRANSACTION_ID);
      assertThat(auditEntity.getAction()).isEqualTo("ERROR");
    }
  }

  @Nested
  @DisplayName("BorrowingPickupRoleIT")
  class BorrowingPickupRoleIT {

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(patron).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode).json",
      "/stubs/mod-inventory-storage/holdings-storage/200-get-by-id.json",
      "/stubs/mod-circulation/requests/201-post(any).json"
    })
    void createTransaction_positive_selfBorrowingAvailableItem() throws Exception {
      var selfBorrowingTransaction = borrowingPickupDcbTransaction(true);

      postDcbTransaction(DCB_TRANSACTION_ID, selfBorrowingTransaction)
        .andExpect(jsonPath("$.status").value("CREATED"));

      verifyPostCirculationRequestCalledOnce(PAGE.getValue(), PICKUP_SERVICE_POINT_ID);
      auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(patron).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode+in-transit).json",
      "/stubs/mod-inventory-storage/holdings-storage/200-get-by-id.json",
      "/stubs/mod-circulation/requests/201-post(any).json"
    })
    void createTransaction_positive_selfBorrowingPageRequestItemInTransit() throws Exception {
      var selfBorrowingTransaction = borrowingPickupDcbTransaction(true);

      postDcbTransaction(DCB_TRANSACTION_ID, selfBorrowingTransaction)
        .andExpect(jsonPath("$.status").value("CREATED"));

      verifyPostCirculationRequestCalledOnce(HOLD.getValue(), PICKUP_SERVICE_POINT_ID);
      auditEntityVerifier.assertThatLatestEntityIsNotDuplicate(DCB_TRANSACTION_ID);
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(patron).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(id+barcode empty).json"
    })
    void createTransaction_negative_selfBorrowingItemNotFound() throws Exception {
      var selfBorrowingTransaction = borrowingPickupDcbTransaction(true);

      postDcbTransactionAttempt(DCB_TRANSACTION_ID, selfBorrowingTransaction)
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND_ERROR"))
        .andExpect(jsonPath("$.errors[0].message").value(("Unable to find existing "
          + "item with id %s and barcode DCB_ITEM.").formatted(ITEM_ID)));

      var auditEntity = auditEntityVerifier.getLatestAuditEntity(DCB_TRANSACTION_ID);
      assertThat(auditEntity.getAction()).isEqualTo("ERROR");
    }

    @Test
    @WireMockStub("/stubs/mod-users/users/200-get-by-query(new_user empty).json")
    void createTransaction_negative_selfBorrowingPatronNotFound() throws Exception {
      var dcbPatron = dcbPatron(NOT_EXISTED_PATRON_ID);
      var selfBorrowingTransaction = borrowingPickupDcbTransaction(dcbPatron, true);

      postDcbTransactionAttempt(DCB_TRANSACTION_ID, selfBorrowingTransaction)
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND_ERROR"))
        .andExpect(jsonPath("$.errors[0].message").value(("Unable to find existing user with"
          + " barcode DCB_PATRON and id %s.").formatted(NOT_EXISTED_PATRON_ID)));

      var auditEntity = auditEntityVerifier.getLatestAuditEntity(DCB_TRANSACTION_ID);
      assertThat(auditEntity.getAction()).isEqualTo("ERROR");
    }
  }

  private static void  verifyPostCirculationRequestCalledOnce(String type, String servicePointId) {
    wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/circulation/requests"))
      .withRequestBody(matchingJsonPath("$.requestType", equalTo(type)))
      .withRequestBody(matchingJsonPath("$.itemId", equalTo(ITEM_ID)))
      .withRequestBody(matchingJsonPath("$.instanceId", equalTo(INSTANCE_ID)))
      .withRequestBody(matchingJsonPath("$.requesterId", equalTo(PATRON_TYPE_USER_ID)))
      .withRequestBody(matchingJsonPath("$.pickupServicePointId", equalTo(servicePointId)))
      .withRequestBody(matchingJsonPath("$.holdingsRecordId", equalTo(HOLDING_RECORD_ID))));
  }
}
