package org.folio.dcb.service;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.AWAITING_PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_IN;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_OUT;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.createTransactionResponse;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.client.feign.CirculationLoanPolicyStorageClient;
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
import org.folio.dcb.repository.TransactionAuditRepository;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.LendingLibraryServiceImpl;
import org.folio.dcb.service.impl.TransactionsServiceImpl;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  @InjectMocks
  private TransactionsServiceImpl transactionsService;
  @Mock(name = "lendingLibraryService")
  private LendingLibraryServiceImpl lendingLibraryService;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private StatusProcessorService statusProcessorService;
  @Mock
  private TransactionAuditRepository transactionAuditRepository;
  @Mock
  private TransactionMapper transactionMapper;
  @Mock
  private CirculationClient circulationClient;
  @Mock
  private CirculationLoanPolicyStorageClient circulationLoanPolicyStorageClient;

  @Test
  void renewLoanByTransactionIdTest() {
    when(transactionRepository.findById(anyString())).thenReturn(Optional.ofNullable(
      buildTransactionToRenew(ITEM_CHECKED_OUT, LENDER)));
    when(circulationClient.renewById(any())).thenReturn(buildTestRenewBleResponse());
    when(circulationLoanPolicyStorageClient.fetchLoanPolicyById(anyString())).thenReturn(
      buildTestLoanPolicy());
    when(circulationClient.renewById(any())).thenReturn(buildTestRenewBleResponse());
    when(circulationLoanPolicyStorageClient.fetchLoanPolicyById(anyString())).thenReturn(
      buildTestLoanPolicy());

    transactionsService.renewLoanByTransactionId(DCB_TRANSACTION_ID);
    verify(circulationClient, times(1)).renewById(any());
    verify(circulationLoanPolicyStorageClient, times(1)).fetchLoanPolicyById(any());
  }

  @Test
  void renewLoanByTransactionIdShouldThrowNotFoundExceptionWhenRenewResponseNull() {
    when(circulationClient.renewById(any(RenewByIdRequest.class))).thenReturn(null);
    when(transactionRepository.findById(anyString())).thenReturn(Optional.of(
      buildTransactionToRenew(ITEM_CHECKED_OUT, LENDER)));
    assertThrows(NotFoundException.class, () -> transactionsService.renewLoanByTransactionId(
      DCB_TRANSACTION_ID));
  }

  @Test
  void renewLoanByTransactionIdShouldThrowNotFoundExceptionWhenLoanPolicyResponseNull() {
    when(transactionRepository.findById(anyString())).thenReturn(Optional.ofNullable(
      buildTransactionToRenew(ITEM_CHECKED_OUT, LENDER)));
    when(circulationClient.renewById(any())).thenReturn(buildTestRenewBleResponse());
    when(circulationLoanPolicyStorageClient.fetchLoanPolicyById(anyString())).thenReturn(null);
    assertThrows(NotFoundException.class, () -> transactionsService.renewLoanByTransactionId(
      DCB_TRANSACTION_ID));
  }

  @ParameterizedTest
  @MethodSource
  void renewLoanByTransactionIdShouldThrowExceptionTest(TransactionEntity transaction,
    Class<Throwable> exception) {
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
      Arguments.of(buildTransactionToRenew(ITEM_CHECKED_OUT, BORROWER),
        IllegalArgumentException.class),
      Arguments.of(buildTransactionToRenew(ITEM_CHECKED_OUT, BORROWING_PICKUP),
        IllegalArgumentException.class)
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
    when(lendingLibraryService.createCirculation(any(), any()))
      .thenReturn(createTransactionResponse());
    transactionsService.createCirculationRequest(DCB_TRANSACTION_ID,
      createDcbTransactionByRole(LENDER));
    verify(lendingLibraryService).createCirculation(DCB_TRANSACTION_ID,
      createDcbTransactionByRole(LENDER));
  }

  @Test
  void shouldReturnAnyTransactionStatusById() {
    var transactionIdUnique = UUID.randomUUID().toString();
    when(transactionRepository.findById(transactionIdUnique))
      .thenReturn(Optional.ofNullable(TransactionEntity.builder()
        .status(StatusEnum.CREATED)
        .role(LENDER)
        .build()));

    var trnInstance = transactionsService.getTransactionStatusById(transactionIdUnique);
    assertNotNull(trnInstance);
    assertEquals(TransactionStatusResponse.StatusEnum.CREATED, trnInstance.getStatus());
  }

  @Test
  void getTransactionStatusByIdNotFoundExceptionTest() {
    var transactionIdUnique = UUID.randomUUID().toString();
    when(transactionRepository.findById(transactionIdUnique))
      .thenReturn(Optional.empty());

    Throwable exception = assertThrows(
      NotFoundException.class,
      () -> transactionsService.getTransactionStatusById(transactionIdUnique)
    );

    Assertions.assertEquals(
      String.format("DCB Transaction was not found by id= %s ", transactionIdUnique),
      exception.getMessage());
  }

  /**
   * For any kind of role: LENDER/BORROWER/PICKUP/BORROWING_PICKUP
   */
  @Test
  void createTransactionWithExistingTransactionIdTest() {
    var dcbTransaction = createDcbTransactionByRole(LENDER);
    when(transactionRepository.existsById(DCB_TRANSACTION_ID)).thenReturn(true);
    Assertions.assertThrows(ResourceAlreadyExistException.class, () ->
      transactionsService.createCirculationRequest(DCB_TRANSACTION_ID, dcbTransaction));
  }

  @Test
  void updateTransactionEntityLenderTest() {
    var dcbTransactionEntity = createTransactionEntity();
    dcbTransactionEntity.setStatus(StatusEnum.OPEN);
    dcbTransactionEntity.setRole(LENDER);

    when(transactionRepository.findById(DCB_TRANSACTION_ID)).thenReturn(
      Optional.of(dcbTransactionEntity));
    when(statusProcessorService.lendingChainProcessor(StatusEnum.OPEN, ITEM_CHECKED_OUT))
      .thenReturn(List.of(StatusEnum.AWAITING_PICKUP, ITEM_CHECKED_OUT));
    doNothing().when(lendingLibraryService)
      .updateTransactionStatus(dcbTransactionEntity, TransactionStatus.builder().status(
        StatusEnum.AWAITING_PICKUP).build());
    doNothing().when(lendingLibraryService)
      .updateTransactionStatus(dcbTransactionEntity, TransactionStatus.builder().status(
        ITEM_CHECKED_OUT).build());

    transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID,
      TransactionStatus.builder().status(
        ITEM_CHECKED_OUT).build());

    verify(statusProcessorService).lendingChainProcessor(StatusEnum.OPEN, ITEM_CHECKED_OUT);
  }

  @Test
  void updateTransactionEntityErrorTest() {
    var dcbTransactionEntity = createTransactionEntity();
    dcbTransactionEntity.setStatus(StatusEnum.OPEN);
    when(transactionRepository.findById(DCB_TRANSACTION_ID)).thenReturn(
      Optional.of(dcbTransactionEntity));
    var openTransactionStatus = TransactionStatus.builder().status(StatusEnum.OPEN).build();

    Assertions.assertThrows(StatusException.class,
      () -> transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID, openTransactionStatus));

    dcbTransactionEntity.setStatus(StatusEnum.ITEM_CHECKED_IN);
    var cancelledTransactionStatus = TransactionStatus.builder()
      .status(StatusEnum.CANCELLED)
      .build();
    Assertions.assertThrows(StatusException.class,
      () -> transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID,
        cancelledTransactionStatus));

    dcbTransactionEntity.setStatus(ITEM_CHECKED_OUT);
    Assertions.assertThrows(StatusException.class,
      () -> transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID,
        cancelledTransactionStatus));
  }

  @Test
  void getTransactionStatusListTest() {
    var startDate = OffsetDateTime.now().minusDays(1L);
    var endDate = OffsetDateTime.now();
    Page<TransactionAuditEntity> pageMock = mock(Page.class);
    when(transactionAuditRepository.findUpdatedTransactionsByDateRange(any(), any(), any()))
      .thenReturn(pageMock);
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

}
