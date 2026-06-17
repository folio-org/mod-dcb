package org.folio.dcb.service;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.AWAITING_PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_IN;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_OUT;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.createTransactionResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.DcbTransaction.RoleEnum;
import org.folio.dcb.domain.dto.LoanPolicy;
import org.folio.dcb.domain.dto.RenewByIdRequest;
import org.folio.dcb.domain.dto.RenewByIdResponse;
import org.folio.dcb.domain.dto.RenewalsPolicy;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatus.StatusEnum;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.dto.TransactionStatusResponseList;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.exception.StatusException;
import org.folio.dcb.integration.circstorage.CirculationLoanPolicyStorageClient;
import org.folio.dcb.integration.circulation.CirculationClient;
import org.folio.dcb.repository.TransactionAuditRepository;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.folio.dcb.service.impl.LendingLibraryServiceImpl;
import org.folio.dcb.service.impl.TransactionsServiceImpl;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  private TransactionsServiceImpl transactionsService;
  @Mock private LendingLibraryServiceImpl lendingLibraryService;
  @Mock private TransactionRepository transactionRepository;
  @Mock private StatusProcessorService statusProcessorService;
  @Mock private TransactionAuditRepository transactionAuditRepository;
  @Mock private TransactionMapper transactionMapper;
  @Mock private CirculationClient circulationClient;
  @Mock private CirculationLoanPolicyStorageClient circulationLoanPolicyStorageClient;
  @Mock private LibraryService pickupLibraryService;
  @Mock private LibraryService borrowingPickupLibraryService;
  @Mock private LibraryService borrowingLibraryService;
  @Mock private BaseLibraryService baseLibraryService;

  @BeforeEach
  void setUp() {
    transactionsService = new TransactionsServiceImpl(lendingLibraryService, borrowingPickupLibraryService,
      pickupLibraryService, borrowingLibraryService, transactionRepository, statusProcessorService, transactionMapper,
      transactionAuditRepository, baseLibraryService, circulationClient, circulationLoanPolicyStorageClient);
  }

  @Test
  void renewLoanByTransactionIdTest() {
    var entityByIdResult = Optional.ofNullable(buildTransactionToRenew(ITEM_CHECKED_OUT, LENDER));
    when(transactionRepository.findById(anyString())).thenReturn(entityByIdResult);
    when(circulationClient.renewById(any())).thenReturn(buildTestRenewBleResponse());
    when(circulationLoanPolicyStorageClient.getById(anyString())).thenReturn(buildTestLoanPolicy());
    when(circulationClient.renewById(any())).thenReturn(buildTestRenewBleResponse());
    when(circulationLoanPolicyStorageClient.getById(anyString())).thenReturn(buildTestLoanPolicy());

    transactionsService.renewLoanByTransactionId(DCB_TRANSACTION_ID);
    verify(circulationClient).renewById(any());
    verify(circulationLoanPolicyStorageClient).getById(any());
  }

  @Test
  void renewLoanByTransactionIdShouldThrowNotFoundExceptionWhenRenewResponseNull() {
    var entityByIdResult = Optional.of(buildTransactionToRenew(ITEM_CHECKED_OUT, LENDER));
    when(circulationClient.renewById(any(RenewByIdRequest.class))).thenReturn(null);
    when(transactionRepository.findById(anyString())).thenReturn(entityByIdResult);
    assertThrows(NotFoundException.class, () -> transactionsService.renewLoanByTransactionId(DCB_TRANSACTION_ID));
  }

  @Test
  void renewLoanByTransactionIdShouldThrowNotFoundExceptionWhenLoanPolicyResponseNull() {
    when(transactionRepository.findById(anyString())).thenReturn(Optional.ofNullable(
      buildTransactionToRenew(ITEM_CHECKED_OUT, LENDER)));
    when(circulationClient.renewById(any())).thenReturn(buildTestRenewBleResponse());
    when(circulationLoanPolicyStorageClient.getById(anyString())).thenReturn(null);
    assertThrows(NotFoundException.class, () -> transactionsService.renewLoanByTransactionId(DCB_TRANSACTION_ID));
  }

  @ParameterizedTest
  @MethodSource
  void renewLoanByTransactionIdShouldThrowExceptionTest(TransactionEntity transaction, Class<Throwable> exception) {
    when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
    assertThrows(exception, () -> transactionsService.renewLoanByTransactionId(DCB_TRANSACTION_ID));
  }

  private static Stream<Arguments> renewLoanByTransactionIdShouldThrowExceptionTest() {
    return Stream.of(
      Arguments.of(buildTransactionToRenew(ITEM_CHECKED_IN, LENDER), StatusException.class),
      Arguments.of(buildTransactionToRenew(OPEN, LENDER), StatusException.class),
      Arguments.of(buildTransactionToRenew(CLOSED, LENDER), StatusException.class),
      Arguments.of(buildTransactionToRenew(AWAITING_PICKUP, LENDER), StatusException.class),
      Arguments.of(buildTransactionToRenew(AWAITING_PICKUP, LENDER), StatusException.class),
      Arguments.of(buildTransactionToRenew(ITEM_CHECKED_OUT, BORROWER), IllegalArgumentException.class),
      Arguments.of(buildTransactionToRenew(ITEM_CHECKED_OUT, BORROWING_PICKUP), IllegalArgumentException.class)
    );
  }

  private static TransactionEntity buildTransactionToRenew(StatusEnum status, RoleEnum role) {
    return TransactionEntity.builder()
      .id(DCB_TRANSACTION_ID)
      .status(status)
      .role(role)
      .build();
  }

  private static LoanPolicy buildTestLoanPolicy() {
    return new LoanPolicy()
      .renewalsPolicy(
        new RenewalsPolicy()
          .numberAllowed(10)
      );
  }

  private static RenewByIdResponse buildTestRenewBleResponse() {
    return new RenewByIdResponse()
      .loanPolicyId("123")
      .renewalCount(2);
  }

  @Test
  void createLendingCirculationRequestTest() {
    when(lendingLibraryService.createCirculation(any(), any())).thenReturn(createTransactionResponse());
    transactionsService.createCirculationRequest(DCB_TRANSACTION_ID, createDcbTransactionByRole(LENDER));
    verify(lendingLibraryService).createCirculation(DCB_TRANSACTION_ID, createDcbTransactionByRole(LENDER));
  }

  @Test
  void shouldReturnAnyTransactionStatusById() {
    var transactionIdUnique = UUID.randomUUID().toString();
    var transactionEntity = TransactionEntity.builder()
      .status(CREATED)
      .role(LENDER)
      .build();
    when(transactionRepository.findById(transactionIdUnique)).thenReturn(Optional.ofNullable(transactionEntity));

    var trnInstance = transactionsService.getTransactionStatusById(transactionIdUnique);

    assertThat(trnInstance).isNotNull();
    assertThat(trnInstance.getStatus()).isEqualTo(TransactionStatusResponse.StatusEnum.CREATED);
  }

  @Test
  void getTransactionStatusByIdNotFoundExceptionTest() {
    var transactionIdUnique = UUID.randomUUID().toString();
    when(transactionRepository.findById(transactionIdUnique)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> transactionsService.getTransactionStatusById(transactionIdUnique))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DCB Transaction was not found by id= %s ", transactionIdUnique);
  }

  /**
   * For any kind of role: LENDER/BORROWER/PICKUP/BORROWING_PICKUP.
   */
  @Test
  void createTransactionWithExistingTransactionIdTest() {
    var dcbTransaction = createDcbTransactionByRole(LENDER);
    when(transactionRepository.existsById(DCB_TRANSACTION_ID)).thenReturn(true);
    assertThatThrownBy(() -> transactionsService.createCirculationRequest(DCB_TRANSACTION_ID, dcbTransaction))
      .isInstanceOf(ResourceAlreadyExistException.class);
  }

  @Test
  void updateTransactionEntityLenderTest() {
    var dcbTransactionEntity = createTransactionEntity();
    dcbTransactionEntity.setStatus(StatusEnum.OPEN);
    dcbTransactionEntity.setRole(LENDER);

    when(transactionRepository.findById(DCB_TRANSACTION_ID)).thenReturn(Optional.of(dcbTransactionEntity));
    when(statusProcessorService.lendingChainProcessor(StatusEnum.OPEN, ITEM_CHECKED_OUT))
      .thenReturn(List.of(StatusEnum.AWAITING_PICKUP, ITEM_CHECKED_OUT));
    doNothing().when(lendingLibraryService).updateTransactionStatus(dcbTransactionEntity,
      TransactionStatus.builder().status(StatusEnum.AWAITING_PICKUP).build());
    var itemCheckedOutStatus = TransactionStatus.builder().status(ITEM_CHECKED_OUT).build();
    doNothing().when(lendingLibraryService).updateTransactionStatus(dcbTransactionEntity, itemCheckedOutStatus);

    transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID, itemCheckedOutStatus);

    verify(statusProcessorService).lendingChainProcessor(StatusEnum.OPEN, ITEM_CHECKED_OUT);
  }

  @Test
  void updateTransactionEntityErrorTest() {
    var dcbTransactionEntity = createTransactionEntity();
    dcbTransactionEntity.setStatus(StatusEnum.OPEN);
    when(transactionRepository.findById(DCB_TRANSACTION_ID)).thenReturn(Optional.of(dcbTransactionEntity));
    var openTransactionStatus = TransactionStatus.builder().status(StatusEnum.OPEN).build();

    assertThatThrownBy(() -> transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID, openTransactionStatus))
      .isInstanceOf(StatusException.class);

    dcbTransactionEntity.setStatus(StatusEnum.ITEM_CHECKED_IN);
    var cancelledTxStatus = TransactionStatus.builder().status(StatusEnum.CANCELLED).build();
    assertThatThrownBy(() -> transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID, cancelledTxStatus))
      .isInstanceOf(StatusException.class);

    dcbTransactionEntity.setStatus(ITEM_CHECKED_OUT);
    assertThatThrownBy(() -> transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID, cancelledTxStatus))
      .isInstanceOf(StatusException.class);
  }

  @Test
  void getTransactionStatusListTest() {
    var startDate = OffsetDateTime.now().minusDays(1L);
    var endDate = OffsetDateTime.now();
    var pageMock = Mockito.<Page<TransactionAuditEntity>>mock();
    when(transactionAuditRepository.findUpdatedTransactionsByDateRange(any(), any(), any())).thenReturn(pageMock);
    when(pageMock.getTotalElements()).thenReturn(10L);
    when(transactionMapper.mapToDto(pageMock))
      .thenReturn(List.of(TransactionStatusResponseList
        .builder()
        .id(DCB_TRANSACTION_ID)
        .status(TransactionStatusResponseList.StatusEnum.ITEM_CHECKED_OUT)
        .role(TransactionStatusResponseList.RoleEnum.LENDER)
        .build()));
    var response = transactionsService.getTransactionStatusList(startDate, endDate, 0, 3);
    assertEquals(0, response.getCurrentPageNumber());
    assertEquals(3, response.getCurrentPageSize());
    assertEquals(3, response.getMaximumPageNumber());
    assertEquals(10, response.getTotalRecords());

    response = transactionsService.getTransactionStatusList(startDate, endDate, 0, 5);
    assertEquals(0, response.getCurrentPageNumber());
    assertEquals(5, response.getCurrentPageSize());
    assertEquals(1, response.getMaximumPageNumber());
    assertEquals(10, response.getTotalRecords());

    response = transactionsService.getTransactionStatusList(startDate, endDate, 4, 2);
    assertEquals(4, response.getCurrentPageNumber());
    assertEquals(2, response.getCurrentPageSize());
    assertEquals(4, response.getMaximumPageNumber());
    assertEquals(10, response.getTotalRecords());

    response = transactionsService.getTransactionStatusList(startDate, endDate, 10, 10);
    assertEquals(10, response.getCurrentPageNumber());
    assertEquals(10, response.getCurrentPageSize());
    assertEquals(0, response.getMaximumPageNumber());
    assertEquals(10, response.getTotalRecords());
  }

  @Test
  void shouldVerifyLocationCodeWhenCreatingCirculationRequest() {
    when(lendingLibraryService.createCirculation(any(), any())).thenReturn(createTransactionResponse());
    var dcbTransaction = createDcbTransactionByRole(LENDER);
    requireNonNull(dcbTransaction.getItem()).setLocationCode("TEST_LOCATION_CODE");
    transactionsService.createCirculationRequest(DCB_TRANSACTION_ID, dcbTransaction);
    verify(lendingLibraryService).createCirculation(DCB_TRANSACTION_ID, dcbTransaction);

    var dcbTransactionArgumentCaptor = ArgumentCaptor.forClass(DcbTransaction.class);
    verify(lendingLibraryService).createCirculation(any(), dcbTransactionArgumentCaptor.capture());
    DcbTransaction dcbTransactionActual = dcbTransactionArgumentCaptor.getValue();
    assertNotNull(dcbTransactionActual.getItem());
    assertNotNull(dcbTransactionActual.getItem().getLocationCode());
    assertEquals("TEST_LOCATION_CODE", dcbTransactionActual.getItem().getLocationCode());
  }

  @Test
  void createCirculationRequestShouldDelegateToBorrowingPickupService() {
    // TestMate-0a2a9520399d5c7abefe2604abee8bb1
    var transaction = createDcbTransactionByRole(BORROWING_PICKUP);
    var expectedResponse = createTransactionResponse();
    when(transactionRepository.existsById(DCB_TRANSACTION_ID)).thenReturn(false);
    when(borrowingPickupLibraryService.createCirculation(DCB_TRANSACTION_ID, transaction)).thenReturn(expectedResponse);

    var actualResponse = transactionsService.createCirculationRequest(DCB_TRANSACTION_ID, transaction);

    assertThat(actualResponse).isEqualTo(expectedResponse);
    verify(transactionRepository).existsById(DCB_TRANSACTION_ID);
    verify(borrowingPickupLibraryService).createCirculation(DCB_TRANSACTION_ID, transaction);
    verifyNoInteractions(lendingLibraryService, pickupLibraryService, borrowingLibraryService);
  }

  @Test
  void createCirculationRequestShouldDelegateToPickupService() {
    // TestMate-597be30571329e3ce0267cdfff9dfb08
    var transaction = createDcbTransactionByRole(PICKUP);
    var expectedResponse = createTransactionResponse();
    when(transactionRepository.existsById(DCB_TRANSACTION_ID)).thenReturn(false);
    when(pickupLibraryService.createCirculation(DCB_TRANSACTION_ID, transaction)).thenReturn(expectedResponse);

    var actualResponse = transactionsService.createCirculationRequest(DCB_TRANSACTION_ID, transaction);

    assertThat(actualResponse).isEqualTo(expectedResponse);
    verify(transactionRepository).existsById(DCB_TRANSACTION_ID);
    verify(pickupLibraryService).createCirculation(DCB_TRANSACTION_ID, transaction);
    verifyNoInteractions(lendingLibraryService, borrowingPickupLibraryService, borrowingLibraryService);
  }

  @Test
  void createCirculationRequestShouldDelegateToBorrowerService() {
    // TestMate-7d4eb902b0fe10bf7674b7be235df7bd
    var dcbTransaction = createDcbTransactionByRole(BORROWER);
    var expectedResponse = createTransactionResponse();
    when(transactionRepository.existsById(DCB_TRANSACTION_ID)).thenReturn(false);
    when(borrowingLibraryService.createCirculation(DCB_TRANSACTION_ID, dcbTransaction)).thenReturn(expectedResponse);

    var actualResponse = transactionsService.createCirculationRequest(DCB_TRANSACTION_ID, dcbTransaction);

    assertThat(actualResponse).isEqualTo(expectedResponse);
    verify(transactionRepository).existsById(DCB_TRANSACTION_ID);
    verify(borrowingLibraryService).createCirculation(DCB_TRANSACTION_ID, dcbTransaction);
    verifyNoInteractions(lendingLibraryService, borrowingPickupLibraryService, pickupLibraryService);
  }

  @Test
  void updateTransactionStatusWhenTransactionNotFoundShouldThrowException() {
    // TestMate-dcfec3a2e25a9cd96051a6cf26412215
    var nonExistentId = UUID.randomUUID().toString();
    when(transactionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

    var status = new TransactionStatus().status(OPEN);
    assertThatThrownBy(() -> transactionsService.updateTransactionStatus(nonExistentId, status))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Transaction with id %s not found", nonExistentId);

    verify(transactionRepository).findById(nonExistentId);
  }

  @Test
  void updateTransactionStatusForBorrowerRoleShouldUseBorrowingChainProcessor() {
    // TestMate-e08c55185fe8d0dcaa3a2fd733caef67
    var dcbTransactionEntity = createTransactionEntity();
    dcbTransactionEntity.setStatus(OPEN);
    dcbTransactionEntity.setRole(BORROWER);
    var statusChain = List.of(AWAITING_PICKUP, ITEM_CHECKED_OUT);
    when(transactionRepository.findById(DCB_TRANSACTION_ID)).thenReturn(Optional.of(dcbTransactionEntity));
    when(statusProcessorService.borrowingChainProcessor(OPEN, ITEM_CHECKED_OUT)).thenReturn(statusChain);
    doNothing().when(borrowingLibraryService).updateTransactionStatus(any(), any());

    var targetStatus = new TransactionStatus().status(ITEM_CHECKED_OUT);
    var response = transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID, targetStatus);

    assertThat(response.getStatus()).isEqualTo(TransactionStatusResponse.StatusEnum.ITEM_CHECKED_OUT);
    verify(statusProcessorService).borrowingChainProcessor(OPEN, ITEM_CHECKED_OUT);
    verify(borrowingLibraryService).updateTransactionStatus(dcbTransactionEntity,
      TransactionStatus.builder().status(AWAITING_PICKUP).build());
    verify(borrowingLibraryService).updateTransactionStatus(dcbTransactionEntity, targetStatus);
    verifyNoInteractions(lendingLibraryService, pickupLibraryService, borrowingPickupLibraryService);
  }

  @Test
  void updateTransactionStatus_positive_borrowingPickupRole() {
    // TestMate-beca7b72cfab01b9d15502559ed19169
    var dcbTransactionEntity = createTransactionEntity();
    dcbTransactionEntity.setId(DCB_TRANSACTION_ID);
    dcbTransactionEntity.setStatus(CREATED);
    dcbTransactionEntity.setRole(BORROWING_PICKUP);
    when(transactionRepository.findById(DCB_TRANSACTION_ID)).thenReturn(Optional.of(dcbTransactionEntity));

    var targetStatus = new TransactionStatus().status(OPEN);
    var response = transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID, targetStatus);

    assertThat(response.getStatus()).isEqualTo(TransactionStatusResponse.StatusEnum.OPEN);
    verify(borrowingPickupLibraryService).updateTransactionStatus(dcbTransactionEntity, targetStatus);
    verifyNoInteractions(pickupLibraryService, statusProcessorService, lendingLibraryService, borrowingLibraryService);
  }

  @Test
  void updateTransactionStatus_positive_pickupRole() {
    // TestMate-beca7b72cfab01b9d15502559ed19169
    var dcbTransactionEntity = createTransactionEntity();
    dcbTransactionEntity.setId(DCB_TRANSACTION_ID);
    dcbTransactionEntity.setStatus(CREATED);
    dcbTransactionEntity.setRole(PICKUP);
    when(transactionRepository.findById(DCB_TRANSACTION_ID)).thenReturn(Optional.of(dcbTransactionEntity));

    var targetStatus = new TransactionStatus().status(OPEN);
    var response = transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID, targetStatus);

    assertThat(response.getStatus()).isEqualTo(TransactionStatusResponse.StatusEnum.OPEN);
    verify(pickupLibraryService).updateTransactionStatus(dcbTransactionEntity, targetStatus);
    verifyNoInteractions(borrowingPickupLibraryService, statusProcessorService,
      lendingLibraryService, borrowingLibraryService);
  }
}
