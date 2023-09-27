package org.folio.dcb.controller;

import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.folio.dcb.utils.EntityUtils.*;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionApiControllerTest extends BaseIT {

  @Autowired
  private TransactionRepository transactionRepository;

  @Autowired
  private SystemUserScopedExecutionService systemUserScopedExecutionService;

  @Test
  void createLendingCirculationRequestTest() throws Exception {
    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransaction()))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(createDcbItem()))
      .andExpect(jsonPath("$.patron").value(createDcbPatron()));

    //Trying to create another transaction with same transaction id
    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransaction()))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));
  }

  @Test
  void createLendingCirculationRequestWithInvalidItemId() throws Exception {
    var dcbTransaction = createDcbTransaction();
    dcbTransaction.getItem().setId("5b95877d-86c0-4cb7-a0cd-7660b348ae5b");

    this.mockMvc.perform(
        post("/transactions/" + UUID.randomUUID())
          .content(asJsonString(dcbTransaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("NOT_FOUND_ERROR")));
  }

  @Test
  void updateLendingDcbTransactionStatusUpdateTest() throws Exception {

    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.OPEN);
    dcbTransaction.setRole(DcbTransaction.RoleEnum.LENDER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("AWAITING_PICKUP"));
  }

}
