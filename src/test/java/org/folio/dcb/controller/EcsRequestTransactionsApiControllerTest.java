package org.folio.dcb.controller;

import static org.folio.dcb.utils.EntityUtils.CIRCULATION_REQUEST_ID;
import static org.folio.dcb.utils.EntityUtils.createBorrowingEcsRequestTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createBorrowingPickupEcsRequestTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createLendingEcsRequestTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createPickupEcsRequestTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.repository.TransactionAuditRepository;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class EcsRequestTransactionsApiControllerTest extends BaseIT {

  private static final String TRANSACTION_AUDIT_DUPLICATE_ERROR_ACTION = "DUPLICATE_ERROR";
  private static final String DUPLICATE_ERROR_TRANSACTION_ID = "-1";

  @Autowired
  private TransactionRepository transactionRepository;
  @Autowired
  private TransactionAuditRepository transactionAuditRepository;
  @Autowired
  private SystemUserScopedExecutionService systemUserScopedExecutionService;

  @Test
  void createLendingEcsRequestTest() throws Exception {
    removeExistedTransactionFromDbIfSoExists();

    this.mockMvc.perform(
      post("/ecs-request-transactions/" + CIRCULATION_REQUEST_ID)
        .content(asJsonString(createLendingEcsRequestTransactionByRole()))
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated());

    //Trying to create another transaction with same transaction id
    this.mockMvc.perform(
      post("/ecs-request-transactions/" + CIRCULATION_REQUEST_ID)
        .content(asJsonString(createLendingEcsRequestTransactionByRole()))
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));

    // check for DUPLICATE_ERROR propagated into transactions_audit.
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(
      TENANT,
      () -> {
        TransactionAuditEntity auditExisting = transactionAuditRepository
          .findLatestTransactionAuditEntityByDcbTransactionId(CIRCULATION_REQUEST_ID)
          .orElse(null);
        Assertions.assertNotNull(auditExisting);
        Assertions.assertNotEquals(TRANSACTION_AUDIT_DUPLICATE_ERROR_ACTION,
          auditExisting.getAction());
        Assertions.assertNotEquals(DUPLICATE_ERROR_TRANSACTION_ID,
          auditExisting.getTransactionId());
      }
    );
  }

  @Test
  void createBorrowingEcsRequestTest() throws Exception {
    removeExistedTransactionFromDbIfSoExists();

    this.mockMvc.perform(
      post("/ecs-request-transactions/" + CIRCULATION_REQUEST_ID)
        .content(asJsonString(createBorrowingEcsRequestTransactionByRole()))
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated());
  }

  @Test
  void createPickupEcsRequestTest() throws Exception {
    removeExistedTransactionFromDbIfSoExists();

    this.mockMvc.perform(
        post("/ecs-request-transactions/" + CIRCULATION_REQUEST_ID)
          .content(asJsonString(createPickupEcsRequestTransactionByRole()))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated());
  }

  @Test
  void createBorrowingPickupEcsRequestTest() throws Exception {
    removeExistedTransactionFromDbIfSoExists();

    this.mockMvc.perform(
        post("/ecs-request-transactions/" + CIRCULATION_REQUEST_ID)
          .content(asJsonString(createBorrowingPickupEcsRequestTransactionByRole()))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated());
  }

  @Test
  void checkErrorStatusForInvalidRequest() throws Exception {
    DcbTransaction dcbTransaction = createLendingEcsRequestTransactionByRole();
    dcbTransaction.setRequestId(UUID.randomUUID().toString());
    this.mockMvc.perform(
      post("/ecs-request-transactions/" + CIRCULATION_REQUEST_ID)
        .content(asJsonString(dcbTransaction))
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError());
  }

  @Test
  void updateTransactionWithItemBarcode() throws Exception {
    String realItemBarcode = "real_item_barcode";
    String transactionId = UUID.randomUUID().toString();
    String itemId = UUID.randomUUID().toString();
    TransactionEntity transaction = createTransactionEntity();
    transaction.setId(transactionId);
    transaction.setStatus(TransactionStatus.StatusEnum.OPEN);
    transaction.setItemId(itemId);
    transaction.setItemBarcode(itemId); // itemId used as barcode intentionally

    systemUserScopedExecutionService.executeSystemUserScoped(TENANT,
      () -> transactionRepository.save(transaction));

    DcbTransaction requestBody = new DcbTransaction()
      .item(new DcbItem().barcode(realItemBarcode));

    mockMvc.perform(
      patch("/ecs-request-transactions/" + transactionId)
        .content(asJsonString(requestBody))
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());

    String updatedTransactionItemBarcode = systemUserScopedExecutionService.executeSystemUserScoped(
        TENANT, () -> transactionRepository.findById(transactionId))
      .orElseThrow()
      .getItemBarcode();
    assertEquals(realItemBarcode, updatedTransactionItemBarcode);
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

  private void removeExistedTransactionFromDbIfSoExists() {
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () -> {
      if (transactionRepository.existsById(CIRCULATION_REQUEST_ID)) {
        transactionRepository.deleteById(CIRCULATION_REQUEST_ID);
      }
    });
  }
}
