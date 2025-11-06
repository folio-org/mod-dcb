package org.folio.dcb.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatusResponse.StatusEnum.ITEM_CHECKED_OUT;
import static org.folio.dcb.utils.EntityUtils.DCB_NEW_BARCODE;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.DCB_TYPE_USER_ID;
import static org.folio.dcb.utils.EntityUtils.EXISTED_INVENTORY_ITEM_BARCODE;
import static org.folio.dcb.utils.EntityUtils.EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.HOLDING_RECORD_ID;
import static org.folio.dcb.utils.EntityUtils.INSTANCE_ID;
import static org.folio.dcb.utils.EntityUtils.ITEM_ID;
import static org.folio.dcb.utils.EntityUtils.ITEM_ID_STATUS_NOT_AVAILABLE;
import static org.folio.dcb.utils.EntityUtils.NOT_EXISTED_ITEM_ID;
import static org.folio.dcb.utils.EntityUtils.NOT_EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.PATRON_TYPE_USER_ID;
import static org.folio.dcb.utils.EntityUtils.createDcbItem;
import static org.folio.dcb.utils.EntityUtils.createDcbPatronWithExactPatronId;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRoleAndSelfBorrowing;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionUpdate;
import static org.folio.dcb.utils.EntityUtils.createDefaultDcbPatron;
import static org.folio.dcb.utils.EntityUtils.createInventoryItem;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.createTransactionStatus;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.client.feign.CirculationLoanPolicyStorageClient;
import org.folio.dcb.client.feign.HoldingsStorageClient;
import org.folio.dcb.client.feign.InventoryItemStorageClient;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.InventoryItem;
import org.folio.dcb.domain.dto.ItemStatus;
import org.folio.dcb.domain.dto.Loan;
import org.folio.dcb.domain.dto.LoanCollection;
import org.folio.dcb.domain.dto.LoanPolicy;
import org.folio.dcb.domain.dto.LoanPolicyCollection;
import org.folio.dcb.domain.dto.RenewByIdRequest;
import org.folio.dcb.domain.dto.RenewByIdResponse;
import org.folio.dcb.domain.dto.RenewByIdResponseBorrower;
import org.folio.dcb.domain.dto.RenewByIdResponseLoanPolicy;
import org.folio.dcb.domain.dto.RenewByIdResponseLostItemPolicy;
import org.folio.dcb.domain.dto.RenewByIdResponseOverdueFinePolicy;
import org.folio.dcb.domain.dto.RenewByIdResponseStatus;
import org.folio.dcb.domain.dto.RenewalInfo;
import org.folio.dcb.domain.dto.RenewalsPolicy;
import org.folio.dcb.domain.dto.Status;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.repository.TransactionAuditRepository;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.RequestService;
import org.folio.spring.model.ResultList;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import com.jayway.jsonpath.JsonPath;

import lombok.SneakyThrows;

class TransactionApiControllerTest extends BaseIT {

  private static final String TRANSACTION_AUDIT_ERROR_ACTION = "ERROR";
  private static final String TRANSACTION_AUDIT_DUPLICATE_ERROR_ACTION = "DUPLICATE_ERROR";
  private static final String DUPLICATE_ERROR_TRANSACTION_ID = "-1";

  private final ArgumentCaptor<CirculationRequest> captor = ArgumentCaptor.forClass(CirculationRequest.class);

  @Autowired
  private TransactionRepository transactionRepository;
  @Autowired
  private TransactionAuditRepository transactionAuditRepository;

  @Autowired
  private SystemUserScopedExecutionService systemUserScopedExecutionService;
  @MockitoSpyBean
  private CirculationClient circulationClient;
  @MockitoSpyBean
  private CirculationLoanPolicyStorageClient circulationLoanPolicyStorageClient;
  @MockitoSpyBean
  private InventoryItemStorageClient inventoryItemStorageClient;
  @MockitoSpyBean
  private HoldingsStorageClient holdingsStorageClient;
  @MockitoSpyBean
  private RequestService requestService;

  @Test
  void createLendingCirculationRequestTest() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionByRole(DcbTransaction.RoleEnum.LENDER)))
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
          .content(asJsonString(createDcbTransactionByRole(DcbTransaction.RoleEnum.LENDER)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));

    // check for DUPLICATE_ERROR propagated into transactions_audit.
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(
      TENANT,
      () -> {
        TransactionAuditEntity auditExisting = transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(
            DCB_TRANSACTION_ID)
          .orElse(null);
        assertNotNull(auditExisting);
        assertNotEquals(TRANSACTION_AUDIT_DUPLICATE_ERROR_ACTION, auditExisting.getAction());
        assertNotEquals(DUPLICATE_ERROR_TRANSACTION_ID, auditExisting.getTransactionId());
      }
    );
  }

  @Test
  void createLendingCirculationRequestTestWithLocalNames() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

    var dcbTransactionByRole = createDcbTransactionByRole(LENDER);
    var patron = createDcbPatronWithExactPatronId(NOT_EXISTED_PATRON_ID).localNames("[John, Doe]");
    dcbTransactionByRole.setPatron(patron);

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(dcbTransactionByRole))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(createDcbItem()))
      .andExpect(jsonPath("$.patron").value(patron));

    wireMockServer.verify(1, postRequestedFor(urlPathMatching(".*/users"))
      .withRequestBody(matchingJsonPath("$.personal.firstName", equalTo("John")))
      .withRequestBody(matchingJsonPath("$.personal.lastName", equalTo("Doe"))));

    // check for DUPLICATE_ERROR propagated into transactions_audit.
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(
      TENANT,
      () -> {
        TransactionAuditEntity auditExisting = transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(
            DCB_TRANSACTION_ID)
          .orElse(null);
        assertNotNull(auditExisting);
        assertNotEquals(TRANSACTION_AUDIT_DUPLICATE_ERROR_ACTION, auditExisting.getAction());
        assertNotEquals(DUPLICATE_ERROR_TRANSACTION_ID, auditExisting.getTransactionId());
      }
    );
  }

  @Test
  void createLendingCirculationRequestWithValidIdAndInvalidBarcode() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    var dcbTransaction = createDcbTransactionByRole(DcbTransaction.RoleEnum.LENDER);
    dcbTransaction.getItem().setBarcode("DCB_ITEM1");

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(dcbTransaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("NOT_FOUND_ERROR")));

  }

  @Test
  void createCirculationRequestWithInvalidUUID() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    var dcbTransaction = createDcbTransactionByRole(DcbTransaction.RoleEnum.LENDER);
    //Setting a non UUID for itemId
    dcbTransaction.getItem().setId("1234");

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(dcbTransaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("VALIDATION_ERROR")));
  }

  @Test
  void createBorrowingPickupCirculationRequestTest() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

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

    // check for DUPLICATE_ERROR propagated into transactions_audit.
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(
      TENANT,
      () -> {
        TransactionAuditEntity auditExisting = transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(
            DCB_TRANSACTION_ID)
          .orElse(null);
        assertNotNull(auditExisting);
        assertNotEquals(TRANSACTION_AUDIT_DUPLICATE_ERROR_ACTION, auditExisting.getAction());
        assertNotEquals(DUPLICATE_ERROR_TRANSACTION_ID, auditExisting.getTransactionId());
      }
    );
  }

  @Test
  void createLendingCirculationRequestWithInvalidItemId() throws Exception {
    var dcbTransaction = createDcbTransactionByRole(DcbTransaction.RoleEnum.LENDER);
    dcbTransaction.getItem().setId("5b95877d-86c0-4cb7-a0cd-7660b348ae5b");

    String trnId = UUID.randomUUID().toString();

    this.mockMvc.perform(
        post("/transactions/" + trnId)
          .content(asJsonString(dcbTransaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("NOT_FOUND_ERROR")));

    // check for transactions_audit error content.
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(
      TENANT,
      () -> {
        TransactionAuditEntity auditExisting = transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(
            trnId)
          .orElse(null);
        assertNotNull(auditExisting);
        assertEquals(TRANSACTION_AUDIT_ERROR_ACTION, auditExisting.getAction());
      }
    );
  }

  @Test
  void createBorrowingPickupCirculationRequestWithInvalidDefaultNotExistedPatronId()
    throws Exception {
    var dcbTransaction = createDcbTransactionByRole(BORROWING_PICKUP);
    dcbTransaction.getPatron().setId(NOT_EXISTED_PATRON_ID);

    String trnId = UUID.randomUUID().toString();
    this.mockMvc.perform(
        post("/transactions/" + trnId)
          .content(asJsonString(dcbTransaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("NOT_FOUND_ERROR")));

    // check for transactions_audit error content.
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(
      TENANT,
      () -> {
        TransactionAuditEntity auditExisting = transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(
            trnId)
          .orElse(null);
        assertNotNull(auditExisting);
        assertEquals(TRANSACTION_AUDIT_ERROR_ACTION, auditExisting.getAction());
      }
    );
  }

  /**
   * The test at the put endpoint invocation stage initiates stage verification from OPEN to AWAITING_PICKUP
   */
  @Test
  void transactionStatusUpdateFromOpenToAwaitingTest() throws Exception {

    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.OPEN);
    dcbTransaction.setRole(DcbTransaction.RoleEnum.LENDER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(
            asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("AWAITING_PICKUP"));
  }

  @Test
  void transactionStatusUpdateFromOpenToCheckedInTest() throws Exception {

    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.OPEN);
    dcbTransaction.setRole(DcbTransaction.RoleEnum.LENDER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(
            asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ITEM_CHECKED_IN"));
  }

  @Test
  void transactionStatusCancelledFromClosedTest() throws Exception {

    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.OPEN);
    dcbTransaction.setRole(BORROWER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.CLOSED)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("CLOSED"));

    // Trying to Cancel the closed transaction
    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.CANCELLED)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("VALIDATION_ERROR")));
  }

  @Test
  void transactionStatusUpdateFromCreatedToOpenForPickupLibTest() throws Exception {

    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.CREATED);
    dcbTransaction.setRole(PICKUP);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

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
   */
  @Test
  void transactionStatusUpdateFromAwaitingToCheckedOutTest() throws Exception {

    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    dcbTransaction.setRole(DcbTransaction.RoleEnum.LENDER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(
            asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ITEM_CHECKED_OUT"));
  }

  /**
   * The test at the put endpoint invocation stage initiates stage verification from CHECKED_OUT to CHECKED_IN
   */
  @Test
  void transactionStatusUpdateFromCheckOutToCheckInTest() throws Exception {

    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    dcbTransaction.setRole(DcbTransaction.RoleEnum.LENDER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(
            asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN)))
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

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

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
   */
  @Test
  void getLendingTransactionStatusSuccessTest() throws Exception {
    var id = UUID.randomUUID().toString();
    this.mockMvc.perform(
        post("/transactions/" + id)
          .content(asJsonString(createDcbTransactionByRole(DcbTransaction.RoleEnum.LENDER)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(createDcbItem()))
      .andExpect(jsonPath("$.patron").value(createDefaultDcbPatron()));

    mockMvc.perform(
        get("/transactions/" + id + "/status")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.item").exists())
      .andExpect(jsonPath("$.item.holdCount", is(5)));
  }

  private static Stream<Arguments> transactionRoles() {
    return Stream.of(
      Arguments.of(BORROWER),
      Arguments.of(BORROWING_PICKUP)
    );
  }

  @ParameterizedTest
  @MethodSource("transactionRoles")
  void getLendingTransactionStatusSuccessTestRenewalCountUnlimitedFalse(
    DcbTransaction.RoleEnum role) throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    dcbTransaction.setRole(role);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    String randomUUID = UUID.randomUUID().toString();

    LoanCollection loanCollection = LoanCollection.builder()
      .loans(List.of(Loan.builder()
        .id(randomUUID)
        .loanPolicyId(randomUUID)
        .renewalCount("8")
        .status(Status.builder().name("OPEN").build()).build()))
      .totalRecords(1)
      .build();
    Mockito.doReturn(loanCollection).when(circulationClient).fetchLoanByQuery(anyString());
    LoanPolicyCollection loanPolicyCollection = LoanPolicyCollection.builder()
      .loanPolicies(List.of(LoanPolicy.builder()
        .id(randomUUID)
        .renewable(true)
        .renewalsPolicy(RenewalsPolicy.builder()
          .unlimited(false)
          .numberAllowed(22)
          .build()).build()))
      .totalRecords(1)
      .build();
    Mockito.doReturn(loanPolicyCollection)
      .when(circulationLoanPolicyStorageClient)
      .fetchLoanPolicyByQuery(anyString());

    mockMvc.perform(
        get("/transactions/" + transactionID + "/status")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.item.renewalInfo.renewalCount").value(8))
      .andExpect(jsonPath("$.item.renewalInfo.renewable").value(true))
      .andExpect(jsonPath("$.item.renewalInfo.renewalMaxCount").value(22))
      .andExpect(jsonPath("$.item.holdCount").doesNotExist())
    ;
  }

  @Test
  @SneakyThrows
  void renewItemLoanByTransactionId() {
    String transactionId = UUID.randomUUID().toString();
    String itemId = UUID.randomUUID().toString();
    String userId = UUID.randomUUID().toString();
    String loanPolicyId = UUID.randomUUID().toString();

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(buildTransactionFoRenew(transactionId, itemId, userId)));

    RenewByIdResponse renewalResponse = createTestInstance(loanPolicyId);
    RenewByIdRequest renewByIdRequest = buildRenewByIdResponse(itemId, userId);

    doReturn(renewalResponse).when(circulationClient).renewById(renewByIdRequest);
    doReturn(buildTestLoanPolicy()).when(circulationLoanPolicyStorageClient)
      .fetchLoanPolicyById(loanPolicyId);

    TransactionStatusResponse actual = new ObjectMapper().readValue(performRenewById(transactionId),
      TransactionStatusResponse.class);

    assertEquals(buildExpectedResponse(), actual);
  }

  private static TransactionStatusResponse buildExpectedResponse() {
    return TransactionStatusResponse.builder()
      .item(
        DcbItem.builder()
          .renewalInfo(
            RenewalInfo.builder()
              .renewable(true)
              .renewalMaxCount(10)
              .renewalCount(1)
              .build()
          )
          .holdCount(5)
          .build()
      )
      .status(ITEM_CHECKED_OUT)
      .role(TransactionStatusResponse.RoleEnum.LENDER)
      .build();
  }

  private String performRenewById(String transactionId) throws Exception {
    return mockMvc.perform(
        put("/transactions/" + transactionId + "/renew")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();
  }

  private static TransactionEntity buildTransactionFoRenew(String transactionId, String itemId,
    String userId) {
    TransactionEntity transaction = createTransactionEntity();
    transaction.setId(transactionId);
    transaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    transaction.setRole(DcbTransaction.RoleEnum.LENDER);
    transaction.setItemId(itemId);
    transaction.setPatronId(userId);
    return transaction;
  }

  private static LoanPolicy buildTestLoanPolicy() {
    return LoanPolicy.builder()
      .renewable(true)
      .renewalsPolicy(
        RenewalsPolicy.builder()
          .numberAllowed(10)
          .unlimited(false)
          .build()
      ).build();
  }

  private static RenewByIdRequest buildRenewByIdResponse(String itemId, String userId) {
    return RenewByIdRequest.builder()
      .itemId(itemId)
      .userId(userId)
      .build();
  }

  public static RenewByIdResponse createTestInstance(String loanPolicyId) {
    return RenewByIdResponse.builder()
      .id("test-id")
      .userId("test-user-id")
      .borrower(new RenewByIdResponseBorrower())
      .proxyUserId("test-proxy-user-id")
      .itemId("test-item-id")
      .loanPolicyId(loanPolicyId)
      .loanPolicy(new RenewByIdResponseLoanPolicy())
      .overdueFinePolicyId("test-overdue-fine-policy-id")
      .overdueFinePolicy(new RenewByIdResponseOverdueFinePolicy())
      .lostItemPolicyId("test-lost-item-policy-id")
      .lostItemPolicy(new RenewByIdResponseLostItemPolicy())
      .itemEffectiveLocationIdAtCheckOut("test-location-id")
      .status(new RenewByIdResponseStatus())
      .loanDate(OffsetDateTime.now())
      .dueDate(OffsetDateTime.now().plusDays(14))
      .returnDate(OffsetDateTime.now().plusDays(21))
      .systemReturnDate(OffsetDateTime.now().plusDays(21))
      .action("test-action")
      .actionComment("test-action-comment")
      .renewalCount(1)
      .dueDateChangedByRecall(false)
      .build();
  }

  @ParameterizedTest
  @MethodSource("transactionRoles")
  void getLendingTransactionStatusSuccessTestRenewalCountUnlimitedTrue(DcbTransaction.RoleEnum role)
    throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    dcbTransaction.setRole(role);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    String randomUUID = UUID.randomUUID().toString();
    LoanCollection loanCollection = LoanCollection.builder()
      .loans(List.of(Loan.builder()
        .id(randomUUID)
        .loanPolicyId(randomUUID)
        .renewalCount("8")
        .status(Status.builder().name("OPEN").build()).build()))
      .totalRecords(1)
      .build();
    Mockito.doReturn(loanCollection).when(circulationClient).fetchLoanByQuery(anyString());
    LoanPolicyCollection loanPolicyCollection = LoanPolicyCollection.builder()
      .loanPolicies(List.of(LoanPolicy.builder()
        .id(randomUUID)
        .renewable(true)
        .renewalsPolicy(RenewalsPolicy.builder().unlimited(true).build()).build()))
      .totalRecords(1)
      .build();
    Mockito.doReturn(loanPolicyCollection)
      .when(circulationLoanPolicyStorageClient)
      .fetchLoanPolicyByQuery(anyString());

    mockMvc.perform(
        get("/transactions/" + transactionID + "/status")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.item.renewalInfo.renewalCount").value(8))
      .andExpect(jsonPath("$.item.renewalInfo.renewable").value(true))
      .andExpect(jsonPath("$.item.renewalInfo.renewalMaxCount").value(-1))
      .andExpect(jsonPath("$.item.holdCount").doesNotExist());
  }

  @ParameterizedTest
  @MethodSource("transactionRoles")
  void getLendingTransactionStatusSuccessTestRenewalCountFalseRenewableLoanPolicy(
    DcbTransaction.RoleEnum role) throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    dcbTransaction.setRole(role);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    String randomUUID = UUID.randomUUID().toString();
    LoanCollection loanCollection = LoanCollection.builder()
      .loans(List.of(Loan.builder()
        .id(randomUUID)
        .loanPolicyId(randomUUID)
        .renewalCount("8")
        .status(Status.builder().name("OPEN").build()).build()))
      .totalRecords(1)
      .build();
    Mockito.doReturn(loanCollection).when(circulationClient).fetchLoanByQuery(anyString());
    LoanPolicyCollection loanPolicyCollection = LoanPolicyCollection.builder()
      .loanPolicies(List.of(LoanPolicy.builder()
        .id(randomUUID)
        .renewable(false).build()))
      .totalRecords(1)
      .build();
    Mockito.doReturn(loanPolicyCollection)
      .when(circulationLoanPolicyStorageClient)
      .fetchLoanPolicyByQuery(anyString());

    mockMvc.perform(
        get("/transactions/" + transactionID + "/status")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.item.renewalInfo.renewalCount").value(8))
      .andExpect(jsonPath("$.item.renewalInfo.renewable").value(false))
      .andExpect(jsonPath("$.item.renewalInfo.renewalMaxCount").doesNotExist())
      .andExpect(jsonPath("$.item.holdCount").doesNotExist());
  }

  @ParameterizedTest
  @MethodSource("transactionRoles")
  void getLendingTransactionStatusSuccessTestRenewalCountZeroWhenNullRenewalCountInLoan(
    DcbTransaction.RoleEnum role) throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    dcbTransaction.setRole(role);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    String randomUUID = UUID.randomUUID().toString();

    LoanCollection loanCollection = LoanCollection.builder()
      .loans(List.of(Loan.builder()
        .id(randomUUID)
        .loanPolicyId(randomUUID)
        .renewalCount(null)
        .status(Status.builder().name("OPEN").build()).build()))
      .totalRecords(1)
      .build();
    Mockito.doReturn(loanCollection).when(circulationClient).fetchLoanByQuery(anyString());
    LoanPolicyCollection loanPolicyCollection = LoanPolicyCollection.builder()
      .loanPolicies(List.of(LoanPolicy.builder()
        .id(randomUUID)
        .renewable(true)
        .renewalsPolicy(RenewalsPolicy.builder()
          .unlimited(false)
          .numberAllowed(22)
          .build()).build()))
      .totalRecords(1)
      .build();
    Mockito.doReturn(loanPolicyCollection)
      .when(circulationLoanPolicyStorageClient)
      .fetchLoanPolicyByQuery(anyString());

    mockMvc.perform(
        get("/transactions/" + transactionID + "/status")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.item.renewalInfo.renewalCount").value(0))
      .andExpect(jsonPath("$.item.renewalInfo.renewable").value(true))
      .andExpect(jsonPath("$.item.renewalInfo.renewalMaxCount").value(22))
      .andExpect(jsonPath("$.item.holdCount").doesNotExist());
  }

  @ParameterizedTest
  @MethodSource("transactionRoles")
  void getLendingTransactionStatusSuccessTestRenewalCountLoanNotExist(DcbTransaction.RoleEnum role)
    throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    dcbTransaction.setRole(role);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    LoanCollection loanCollection = LoanCollection.builder()
      .loans(Collections.emptyList())
      .totalRecords(0)
      .build();
    Mockito.doReturn(loanCollection).when(circulationClient).fetchLoanByQuery(anyString());

    mockMvc.perform(
        get("/transactions/" + transactionID + "/status")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.item.renewalInfo").doesNotExist());
  }

  @Test
  void getLendingTransactionStatusSuccessTestRenewalCountNegativeTest() throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    dcbTransaction.setRole(DcbTransaction.RoleEnum.LENDER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    mockMvc.perform(
        get("/transactions/" + transactionID + "/status")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.item").exists())
      .andExpect(jsonPath("$.item.renewalInfo").doesNotExist())
      .andExpect(jsonPath("$.item.holdCount", is(5)));
  }

  @Test
  void getTransactionStatusNotFoundTest() throws Exception {
    var id = UUID.randomUUID().toString();
    mockMvc.perform(
        get("/transactions/" + id + "/status")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  @Test
  void createTransactionForPickupLibrary() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

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
          .content(asJsonString(createDcbTransactionByRole(DcbTransaction.RoleEnum.LENDER)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));

    // check for DUPLICATE_ERROR propagated into transactions_audit.
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(
      TENANT,
      () -> {
        TransactionAuditEntity auditExisting = transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(
            DCB_TRANSACTION_ID)
          .orElse(null);
        assertNotNull(auditExisting);
        assertNotEquals(TRANSACTION_AUDIT_DUPLICATE_ERROR_ACTION, auditExisting.getAction());
        assertNotEquals(DUPLICATE_ERROR_TRANSACTION_ID, auditExisting.getTransactionId());
      }
    );
  }

  @Test
  void transactionStatusUpdateFromOpenToAwaitingPickup() throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.OPEN);
    dcbTransaction.setRole(BORROWER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(
            asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("AWAITING_PICKUP"));
  }

  @Test
  void createBorrowerCirculationRequestTest() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

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

    // check for DUPLICATE_ERROR propagated into transactions_audit.
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(
      TENANT,
      () -> {
        TransactionAuditEntity auditExisting = transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(
            DCB_TRANSACTION_ID)
          .orElse(null);
        assertNotNull(auditExisting);
        assertNotEquals(TRANSACTION_AUDIT_DUPLICATE_ERROR_ACTION, auditExisting.getAction());
        assertNotEquals(DUPLICATE_ERROR_TRANSACTION_ID, auditExisting.getTransactionId());
      }
    );
  }

  @Test
  void createBorrowerCirculationRequestWithoutExistingItemTest() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

    var dcbTransaction = createDcbTransactionByRole(BORROWER);
    dcbTransaction.getItem().setBarcode("newItem");
    var dcbItem = createDcbItem();
    dcbItem.setBarcode("newItem");

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(dcbTransaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item.barcode").value(dcbItem.getBarcode()))
      .andExpect(jsonPath("$.item.materialType").value(dcbItem.getMaterialType()))
      .andExpect(jsonPath("$.item.lendingLibraryCode").value(dcbItem.getLendingLibraryCode()))
      .andExpect(jsonPath("$.item.title").value(dcbItem.getTitle()))
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

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(
            asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ITEM_CHECKED_OUT"));
  }

  @Test
  void transactionStatusUpdateFromCreatedToItemClosed() throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.CREATED);
    dcbTransaction.setRole(BORROWER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.CLOSED)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("CLOSED"));
  }

  @Test
  void lenderTransactionStatusUpdateFromCreatedToOpen() throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.CREATED);
    dcbTransaction.setRole(DcbTransaction.RoleEnum.LENDER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.OPEN)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("OPEN"));
  }

  @Test
  void transactionStatusUpdateFromItemCheckedOutToItemCheckedIn() throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    dcbTransaction.setRole(BORROWER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(
            asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN)))
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
    dcbTransaction.setRole(DcbTransaction.RoleEnum.LENDER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

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
    dcbTransaction.setRole(DcbTransaction.RoleEnum.LENDER);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.CANCELLED)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createLendingTransactionWithDifferentUserType() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

    var transaction = createDcbTransactionByRole(DcbTransaction.RoleEnum.LENDER);
    transaction.getPatron().setId(PATRON_TYPE_USER_ID);
    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(transaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());

    transaction = createDcbTransactionByRole(DcbTransaction.RoleEnum.LENDER);
    transaction.getPatron().setId(DCB_TYPE_USER_ID);
    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(transaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated());

  }

  @Test
  void createBorrowingTransactionWithDifferentUserType() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

    var transaction = createDcbTransactionByRole(BORROWER);
    transaction.getPatron().setId(DCB_TYPE_USER_ID);

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(transaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());

    transaction = createDcbTransactionByRole(BORROWER);
    transaction.getPatron().setId(PATRON_TYPE_USER_ID);

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(transaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated());

  }

  @Test
  void createBorrowingPickupTransactionWithDifferentUserType() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

    var transaction = createDcbTransactionByRole(BORROWING_PICKUP);
    transaction.getPatron().setId(DCB_TYPE_USER_ID);

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(transaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());

    transaction = createDcbTransactionByRole(BORROWING_PICKUP);
    transaction.getPatron().setId(PATRON_TYPE_USER_ID);

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(transaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated());

  }

  @Test
  void createPickupTransactionWithDifferentUserType() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

    var transaction = createDcbTransactionByRole(PICKUP);
    transaction.getPatron().setId(PATRON_TYPE_USER_ID);

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(transaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());

    transaction = createDcbTransactionByRole(PICKUP);
    transaction.getPatron().setId(DCB_TYPE_USER_ID);

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(transaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated());

  }

  @Test
  void transactionCreationErrorIfInventoryItemExists() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

    var transaction = createDcbTransactionByRole(PICKUP);
    transaction.getItem().setBarcode(EXISTED_INVENTORY_ITEM_BARCODE);

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(transaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isConflict());

    transaction = createDcbTransactionByRole(BORROWING_PICKUP);
    transaction.getItem().setBarcode(EXISTED_INVENTORY_ITEM_BARCODE);

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(transaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isConflict());

    transaction = createDcbTransactionByRole(BORROWER);
    transaction.getItem().setBarcode(EXISTED_INVENTORY_ITEM_BARCODE);

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(transaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isConflict());

    transaction = createDcbTransactionByRole(BORROWING_PICKUP);
    transaction.getItem().setBarcode("DCB_ITEM");

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(transaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated());

  }

  @Test
  void transactionStatusUpdateFromCheckedInToCancelledAsBorrowerPickupTest() throws Exception {
    var transactionID = UUID.randomUUID().toString();
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    dcbTransaction.setRole(BORROWING_PICKUP);
    dcbTransaction.setId(transactionID);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

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

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    mockMvc.perform(
        put("/transactions/" + transactionID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.CANCELLED)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void getTransactionStatusUpdateListTest() throws Exception {
    var startDate1 = OffsetDateTime.now(ZoneOffset.UTC);
    var dcbTransaction = createDcbTransactionByRole(BORROWER);
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(dcbTransaction.getItem().getId());

    // Creating new transaction
    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(dcbTransaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated());

    // Update transaction from CREATED to OPEN
    this.mockMvc.perform(
        put("/transactions/" + DCB_TRANSACTION_ID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.OPEN)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("OPEN"));

    var endDate1 = OffsetDateTime.now(ZoneOffset.UTC);

    // There is one update happened(CREATED -> OPEN), so we will get one record
    this.mockMvc.perform(
        get("/transactions/status")
          .headers(defaultHeaders())
          .param("fromDate", String.valueOf(startDate1))
          .param("toDate", String.valueOf(endDate1))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().is(200))
      .andExpect(jsonPath("$.totalRecords", is(1)))
      .andExpect(jsonPath("$.currentPageNumber", is(0)))
      .andExpect(jsonPath("$.currentPageSize", is(1000)))
      .andExpect(jsonPath("$.maximumPageNumber", is(0)))
      .andExpect(jsonPath("$.transactions[*].status",
        containsInRelativeOrder("OPEN")))
      .andExpect(jsonPath("$.transactions[*].id",
        contains(DCB_TRANSACTION_ID)));

    var startDate2 = OffsetDateTime.now(ZoneOffset.UTC);

    // Update transaction from OPEN to CLOSED
    this.mockMvc.perform(
        put("/transactions/" + DCB_TRANSACTION_ID + "/status")
          .content(asJsonString(createTransactionStatus(TransactionStatus.StatusEnum.CLOSED)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("CLOSED"));

    var endDate2 = OffsetDateTime.now(ZoneOffset.UTC);

    // There are 4 update happened(OPEN -> AWAITING_PICKUP, AWAITING_PICKUP -> ITEM_CHECKED_OUT
    // ITEM_CHECKED_OUT -> ITEM_CHECKED_IN, ITEM_CHECKED_IN -> CLOSED), so we will get 4 record
    this.mockMvc.perform(
        get("/transactions/status")
          .headers(defaultHeaders())
          .param("fromDate", String.valueOf(startDate2))
          .param("toDate", String.valueOf(endDate2))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().is(200))
      .andExpect(jsonPath("$.totalRecords", is(4)))
      .andExpect(jsonPath("$.currentPageNumber", is(0)))
      .andExpect(jsonPath("$.currentPageSize", is(1000)))
      .andExpect(jsonPath("$.maximumPageNumber", is(0)))
      .andExpect(jsonPath("$.transactions[*].status",
        containsInRelativeOrder("AWAITING_PICKUP", "ITEM_CHECKED_OUT", "ITEM_CHECKED_IN",
          "CLOSED")));

    // Now try to get all the records from startDate1 to endDate2 without pageSize(default pageSize)
    this.mockMvc.perform(
        get("/transactions/status")
          .headers(defaultHeaders())
          .param("fromDate", String.valueOf(startDate1))
          .param("toDate", String.valueOf(endDate2))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().is(200))
      .andExpect(jsonPath("$.totalRecords", is(5)))
      .andExpect(jsonPath("$.currentPageNumber", is(0)))
      .andExpect(jsonPath("$.currentPageSize", is(1000)))
      .andExpect(jsonPath("$.maximumPageNumber", is(0)))
      .andExpect(jsonPath("$.transactions[*].status",
        containsInRelativeOrder("OPEN", "AWAITING_PICKUP", "ITEM_CHECKED_OUT", "ITEM_CHECKED_IN",
          "CLOSED")));

    // Now try to get the records based on pagination from startDate1 to endDate2.
    this.mockMvc.perform(
        get("/transactions/status")
          .headers(defaultHeaders())
          .param("fromDate", String.valueOf(startDate1))
          .param("toDate", String.valueOf(endDate2))
          .param("pageNumber", String.valueOf(1))
          .param("pageSize", String.valueOf(2))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().is(200))
      .andExpect(jsonPath("$.totalRecords", is(5)))
      .andExpect(jsonPath("$.currentPageNumber", is(1)))
      .andExpect(jsonPath("$.currentPageSize", is(2)))
      .andExpect(jsonPath("$.maximumPageNumber", is(2)))
      .andExpect(jsonPath("$.transactions[*].status",
        containsInRelativeOrder("ITEM_CHECKED_OUT", "ITEM_CHECKED_IN")));
  }

  @Test
  void createAndUpdateBorrowerTransactionTest() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

    MvcResult result = this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionByRole(BORROWER)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(createDcbItem()))
      .andExpect(jsonPath("$.patron").value(createDcbPatronWithExactPatronId(EXISTED_PATRON_ID)))
      .andReturn(); // Capture the response for assertion

    String responseContent = result.getResponse().getContentAsString();
    String itemId = JsonPath.parse(responseContent).read("$.item.id", String.class);
    String itemBarcode = JsonPath.parse(responseContent).read("$.item.barcode", String.class);
    String lendingLibraryCode = JsonPath.parse(responseContent)
      .read("$.item.lendingLibraryCode", String.class);
    String materialType = JsonPath.parse(responseContent).read("$.item.materialType", String.class);

    //Trying to update the transaction with same transaction id
    this.mockMvc.perform(
        put("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionUpdate()))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().isNoContent());

    // check whether item related data is updated
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(
      TENANT,
      () -> {
        var transactionEntity = transactionRepository.findById(DCB_TRANSACTION_ID)
          .orElse(null);
        assertNotNull(transactionEntity);
        assertNotEquals(itemId, transactionEntity.getItemId());
        assertNotEquals(itemBarcode, transactionEntity.getItemBarcode());
        assertEquals(DCB_NEW_BARCODE, transactionEntity.getItemBarcode());
        assertNotEquals(lendingLibraryCode, transactionEntity.getLendingLibraryCode());
        assertEquals("LEN", transactionEntity.getLendingLibraryCode());
        assertNotEquals(materialType, transactionEntity.getMaterialType());
        assertEquals("DVD", transactionEntity.getMaterialType());
      });
  }

  private void removeExistedTransactionFromDbIfSoExists() {
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () -> {
      if (transactionRepository.existsById(DCB_TRANSACTION_ID)) {
        transactionRepository.deleteById(DCB_TRANSACTION_ID);
      }
    });
  }

  private void removeExistingTransactionsByItemId(String itemId) {
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT, () ->
      transactionRepository.findTransactionsByItemIdAndStatusNotInClosed(UUID.fromString(itemId))
        .forEach(transactionEntity -> transactionRepository.deleteById(transactionEntity.getId()))
    );
  }

  @Test
  void createLendingCirculationRequestTestWithSpecialCharBarcode() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);
    String specialCharBarCode = "!@#$%^^&&*()_=+`~|\\]{}DCB_ITEM/'''";
    DcbTransaction dcbTransaction = createDcbTransactionByRole(DcbTransaction.RoleEnum.LENDER);
    dcbTransaction.getItem().barcode(specialCharBarCode);
    DcbItem expectedDCBItem = createDcbItem().barcode(specialCharBarCode);
    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(dcbTransaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(expectedDCBItem))
      .andExpect(jsonPath("$.patron").value(createDefaultDcbPatron()));
  }

  @Test
  void createBorrowingPickupSelfBorrowingPageRequestCirculationRequestTest() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

    DcbItem expected = createDcbItem();
    InventoryItem inventoryItem = createInventoryItem(ITEM_ID, HOLDING_RECORD_ID, "DCB_ITEM", ItemStatus.NameEnum.AVAILABLE);

    Mockito.doReturn(ResultList.asSinglePage(inventoryItem)).when(inventoryItemStorageClient).fetchItemByQuery(anyString());
    HoldingsStorageClient.Holding holding =
      HoldingsStorageClient.Holding.builder().id(HOLDING_RECORD_ID).instanceId(INSTANCE_ID).build();
    Mockito.doReturn(holding).when(holdingsStorageClient).findHolding(HOLDING_RECORD_ID);

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionByRoleAndSelfBorrowing(BORROWING_PICKUP,
            Boolean.TRUE)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(expected))
      .andExpect(jsonPath("$.patron").value(createDcbPatronWithExactPatronId(EXISTED_PATRON_ID)));

    verify(requestService, times(1)).createRequestBasedOnItemStatus(any(), any(), any());

    verify(circulationClient).createRequest(captor.capture());
    CirculationRequest capturedRequest = captor.getValue();
    assertEquals(HOLDING_RECORD_ID, capturedRequest.getHoldingsRecordId().toString());
    assertEquals(CirculationRequest.RequestTypeEnum.PAGE, capturedRequest.getRequestType());

    //Trying to create another transaction with same transaction id
    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionByRoleAndSelfBorrowing(BORROWING_PICKUP,
            Boolean.TRUE)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));

    // check for DUPLICATE_ERROR propagated into transactions_audit.
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(
      TENANT,
      () -> {
        TransactionAuditEntity auditExisting =
          transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(
              DCB_TRANSACTION_ID)
            .orElse(null);
        assertNotNull(auditExisting);
        assertNotEquals(TRANSACTION_AUDIT_DUPLICATE_ERROR_ACTION, auditExisting.getAction());
        assertNotEquals(DUPLICATE_ERROR_TRANSACTION_ID, auditExisting.getTransactionId());
      }
    );
  }

  @Test
  void createBorrowingPickupSelfBorrowingHoldRequestCirculationRequestTest() throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

    DcbItem expected = createDcbItem();
    InventoryItem inventoryItem =
      createInventoryItem(ITEM_ID_STATUS_NOT_AVAILABLE, HOLDING_RECORD_ID, "DCB_ITEM", ItemStatus.NameEnum.CHECKED_OUT);
    Mockito.doReturn(ResultList.asSinglePage(inventoryItem)).when(inventoryItemStorageClient).fetchItemByQuery(anyString());
    HoldingsStorageClient.Holding holding =
      HoldingsStorageClient.Holding.builder().id(HOLDING_RECORD_ID).instanceId(INSTANCE_ID).build();
    Mockito.doReturn(holding).when(holdingsStorageClient).findHolding(HOLDING_RECORD_ID);

    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionByRoleAndSelfBorrowing(BORROWING_PICKUP,
            Boolean.TRUE)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(expected))
      .andExpect(jsonPath("$.patron").value(createDcbPatronWithExactPatronId(EXISTED_PATRON_ID)));

    verify(requestService, times(1)).createRequestBasedOnItemStatus(any(), any(), any());

    verify(circulationClient).createRequest(captor.capture());
    CirculationRequest capturedRequest = captor.getValue();
    assertEquals(HOLDING_RECORD_ID, capturedRequest.getHoldingsRecordId().toString());
    assertEquals(CirculationRequest.RequestTypeEnum.HOLD, capturedRequest.getRequestType());

    //Trying to create another transaction with same transaction id
    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransactionByRoleAndSelfBorrowing(BORROWING_PICKUP,
            Boolean.TRUE)))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));

    // check for DUPLICATE_ERROR propagated into transactions_audit.
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(
      TENANT,
      () -> {
        TransactionAuditEntity auditExisting =
          transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(
              DCB_TRANSACTION_ID)
            .orElse(null);
        assertNotNull(auditExisting);
        assertNotEquals(TRANSACTION_AUDIT_DUPLICATE_ERROR_ACTION, auditExisting.getAction());
        assertNotEquals(DUPLICATE_ERROR_TRANSACTION_ID, auditExisting.getTransactionId());
      }
    );
  }

  @Test
  void createBorrowingPickupSelfBorrowingCirculationRequestWithInvalidDefaultNotExistedPatronId()
    throws Exception {
    var dcbTransaction = createDcbTransactionByRoleAndSelfBorrowing(BORROWING_PICKUP, Boolean.TRUE);
    dcbTransaction.getPatron().setId(NOT_EXISTED_PATRON_ID);

    String trnId = UUID.randomUUID().toString();
    this.mockMvc.perform(
        post("/transactions/" + trnId)
          .content(asJsonString(dcbTransaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("NOT_FOUND_ERROR")));

    // check for transactions_audit error content.
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(
      TENANT,
      () -> {
        TransactionAuditEntity auditExisting =
          transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(
              trnId)
            .orElse(null);
        assertNotNull(auditExisting);
        assertEquals(TRANSACTION_AUDIT_ERROR_ACTION, auditExisting.getAction());
      }
    );
  }

  @Test
  void createBorrowingPickupSelfBorrowingCirculationRequestWithInvalidDefaultNotExistedItemId()
    throws Exception {
    var dcbTransaction = createDcbTransactionByRoleAndSelfBorrowing(BORROWING_PICKUP, Boolean.TRUE);
    dcbTransaction.getItem().setId(NOT_EXISTED_ITEM_ID);

    String trnId = UUID.randomUUID().toString();
    this.mockMvc.perform(
        post("/transactions/" + trnId)
          .content(asJsonString(dcbTransaction))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("NOT_FOUND_ERROR")));

    // check for transactions_audit error content.
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(
      TENANT,
      () -> {
        TransactionAuditEntity auditExisting =
          transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(
              trnId)
            .orElse(null);
        assertNotNull(auditExisting);
        assertEquals(TRANSACTION_AUDIT_ERROR_ACTION, auditExisting.getAction());
      }
    );
  }

  @ParameterizedTest
  @MethodSource("transactionRoles")
  void blockRenewalForDcbTransaction(DcbTransaction.RoleEnum role) throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setId(DCB_TRANSACTION_ID);
    dcbTransaction.setRole(role);
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/{id}/block-renewal", DCB_TRANSACTION_ID)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var loanId = "d217d4d5-8b2b-496b-8aa5-7e60d530e124";
    wireMockServer.verify(1, putRequestedFor(urlPathEqualTo("/circulation/loans/" + loanId))
      .withRequestBody(matchingJsonPath("$.renewalCount", equalTo("2147483647"))));
    wireMockServer.resetRequests();
  }


  @ParameterizedTest
  @MethodSource("transactionRoles")
  void unblockRenewalForDcbTransaction(DcbTransaction.RoleEnum role) throws Exception {
    removeExistedTransactionFromDbIfSoExists();
    removeExistingTransactionsByItemId(ITEM_ID);

    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setId(DCB_TRANSACTION_ID);
    dcbTransaction.setRole(role);
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT,
      () -> transactionRepository.save(dcbTransaction));

    this.mockMvc.perform(
        put("/transactions/{id}/unblock-renewal", DCB_TRANSACTION_ID)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var loanId = "d217d4d5-8b2b-496b-8aa5-7e60d530e124";
    wireMockServer.verify(1, putRequestedFor(urlPathEqualTo("/circulation/loans/" + loanId))
      .withRequestBody(matchingJsonPath("$.renewalCount", equalTo("0"))));
    wireMockServer.resetRequests();
  }
}
