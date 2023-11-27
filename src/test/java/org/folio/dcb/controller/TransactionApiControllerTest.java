package org.folio.dcb.controller;

import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.NOT_EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.createDcbItem;
import static org.folio.dcb.utils.EntityUtils.createDcbPatronWithExactPatronId;
import static org.folio.dcb.utils.EntityUtils.createDefaultDcbPatron;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.createTransactionStatus;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    removeExistedTransactionFromDbIfSoExists();

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionByRole(LENDER)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(createDcbItem()))
      .andExpect(jsonPath("$.patron").value(createDefaultDcbPatron()));

    //Trying to create another transaction with same transaction id
    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionByRole(LENDER)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));
  }

  @Test
  void createBorrowingPickupCirculationRequestTest() throws Exception {
    removeExistedTransactionFromDbIfSoExists();

    DcbItem expected = createDcbItem();
//    expected.setPickupLocation("3a40852d-49fd-4df2-a1f9-6e2641a6e91f"); // temporary stub

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionByRole(BORROWING_PICKUP)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(expected))
      .andExpect(jsonPath("$.patron").value(createDcbPatronWithExactPatronId(EXISTED_PATRON_ID)));

    //Trying to create another transaction with same transaction id
    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionByRole(BORROWING_PICKUP)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));
  }

  @Test
  void createLendingCirculationRequestWithInvalidItemId() throws Exception {
    var dcbTransaction = createDcbTransactionByRole(LENDER);
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
  void createBorrowingPickupCirculationRequestWithInvalidDefaultNotExistedPatronId() throws Exception {
    var dcbTransaction = createDcbTransactionByRole(BORROWING_PICKUP);
    dcbTransaction.getPatron().setId(NOT_EXISTED_PATRON_ID);

    this.mockMvc.perform(
        post("/transactions/" + UUID.randomUUID())
          .content(asJsonString(dcbTransaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("NOT_FOUND_ERROR")));
  }

    /**
     * The test at the put endpoint invocation stage initiates stage verification from OPEN to AWAITING_PICKUP
     * */
  @Test
  void transactionStatusUpdateFromOpenToAwaitingTest() throws Exception {

    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.OPEN);
    dcbTransaction.setRole(LENDER);
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
  @Test
  void transactionStatusUpdateFromCreatedToOpenForPickupLibTest() throws Exception {

    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.CREATED);
    dcbTransaction.setRole(PICKUP);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.OPEN)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("OPEN"));
  }

  /**
   * The test at the put endpoint invocation stage initiates stage verification from AWAITING_PICKUP to CHECKED_OUT
   * */
  @Test
  void transactionStatusUpdateFromAwaitingToCheckedOutTest() throws Exception {

    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    dcbTransaction.setRole(LENDER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ITEM_CHECKED_OUT"));
  }

  /**
   * The test at the put endpoint invocation stage initiates stage verification from CHECKED_OUT to CHECKED_IN
   * */
  @Test
  void transactionStatusUpdateFromCheckOutToCheckInTest() throws Exception {

    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    dcbTransaction.setRole(LENDER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ITEM_CHECKED_IN"));
  }
  @Test
  void transactionStatusUpdateFromCheckedInToClosedTest() throws Exception {

    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    dcbTransaction.setRole(BORROWING_PICKUP);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.CLOSED)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("CLOSED"));
  }


  /**
   * Initiates the data generation via the post endpoint invocation stage
   * then get stage verifies, the data exists.
   * For LENDER role
   * */
  @Test
  void getLendingTransactionStatusSuccessTest() throws Exception {
    var id = UUID.randomUUID().toString();
    this.mockMvc.perform(
        post("/transactions/" + id)
          .content(asJsonString(createDcbTransactionByRole(LENDER)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(createDcbItem()))
      .andExpect(jsonPath("$.patron").value(createDefaultDcbPatron()));

    mockMvc.perform(
        get("/transactions/"+id+"/status")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  @Test
  void getTransactionStatusNotFoundTest() throws Exception {
    var id = UUID.randomUUID().toString();
    mockMvc.perform(
        get("/transactions/"+id+"/status")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  @Test
  void createTransactionForPickupLibrary() throws Exception {
    removeExistedTransactionFromDbIfSoExists();

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionByRole(PICKUP)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(createDcbItem()))
      .andExpect(jsonPath("$.patron").value(createDefaultDcbPatron()));

    //Trying to create another transaction with same transaction id
    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionByRole(LENDER)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));

  }

  @Test
  void transactionStatusUpdateFromOpenToAwaitingPickup() throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.OPEN);
    dcbTransaction.setRole(BORROWER);
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

  @Test
  void createBorrowerCirculationRequestTest() throws Exception {
    removeExistedTransactionFromDbIfSoExists();

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionByRole(BORROWER)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(createDcbItem()))
      .andExpect(jsonPath("$.patron").value(createDcbPatronWithExactPatronId(EXISTED_PATRON_ID)));

    //Trying to create another transaction with same transaction id
    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionByRole(BORROWER)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));
  }

  @Test
  void transactionStatusUpdateFromAwaitingPickupToItemCheckedOut() throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    dcbTransaction.setRole(BORROWER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ITEM_CHECKED_OUT"));
  }

  @Test
  void transactionStatusUpdateFromItemCheckedOutToItemCheckedIn() throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    dcbTransaction.setRole(BORROWER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ITEM_CHECKED_IN"));
  }

  @Test
  void transactionStatusUpdateFromCreatedToCancelledAsLenderTest() throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.CREATED);
    dcbTransaction.setRole(LENDER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () -> transactionRepository.save(dcbTransaction));

    mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.CANCELLED)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void transactionStatusUpdateFromCheckedInToCancelledAsLenderTest() throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    dcbTransaction.setRole(LENDER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () -> transactionRepository.save(dcbTransaction));

    mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.CANCELLED)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void transactionStatusUpdateFromCheckedInToCancelledAsBorrowerPickupTest() throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    dcbTransaction.setRole(BORROWING_PICKUP);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () -> transactionRepository.save(dcbTransaction));

    mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.CANCELLED)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void transactionStatusUpdateFromCheckedInToCancelledAsPickupTest() throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    dcbTransaction.setRole(PICKUP);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () -> transactionRepository.save(dcbTransaction));

    mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.CANCELLED)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  private void removeExistedTransactionFromDbIfSoExists() {
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () -> {
      if (transactionRepository.existsById(DCB_TRANSACTION_ID)){
        transactionRepository.deleteById(DCB_TRANSACTION_ID);
      }
    });
  }
}
