package org.folio.dcb.service;

import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.exception.StatusException;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.LendingLibraryServiceImpl;
import org.folio.dcb.service.impl.TransactionsServiceImpl;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.createTransactionResponse;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  @InjectMocks
  private TransactionsServiceImpl transactionsService;
  @Mock(name="lendingLibraryService")
  private LendingLibraryServiceImpl lendingLibraryService;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private StatusProcessorService statusProcessorService;

  @Test
  void createLendingCirculationRequestTest() {
    when(lendingLibraryService.createCirculation(any(), any()))
      .thenReturn(createTransactionResponse());
    transactionsService.createCirculationRequest(DCB_TRANSACTION_ID, createDcbTransactionByRole(LENDER));
    verify(lendingLibraryService).createCirculation(DCB_TRANSACTION_ID, createDcbTransactionByRole(LENDER));
  }

  @Test
  void shouldReturnAnyTransactionStatusById(){
    var transactionIdUnique = UUID.randomUUID().toString();
    when(transactionRepository.findById(transactionIdUnique))
      .thenReturn(Optional.ofNullable(TransactionEntity.builder()
        .status(TransactionStatus.StatusEnum.CREATED)
        .role(LENDER)
        .build()));

    var trnInstance = transactionsService.getTransactionStatusById(transactionIdUnique);
    assertNotNull(trnInstance);
    assertEquals(TransactionStatusResponse.StatusEnum.CREATED, trnInstance.getStatus());
  }

  @Test
  void getTransactionStatusByIdNotFoundExceptionTest(){
    var transactionIdUnique = UUID.randomUUID().toString();
    when(transactionRepository.findById(transactionIdUnique))
      .thenReturn(Optional.empty());

    Throwable exception = assertThrows(
      NotFoundException.class, () -> transactionsService.getTransactionStatusById(transactionIdUnique)
    );

    Assertions.assertEquals(String.format("DCB Transaction was not found by id= %s ", transactionIdUnique), exception.getMessage());
  }

  /**
   * For any kind of role: LENDER/BORROWER/PICKUP/BORROWING_PICKUP
   * */
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
    dcbTransactionEntity.setStatus(TransactionStatus.StatusEnum.OPEN);
    dcbTransactionEntity.setRole(LENDER);

    when(transactionRepository.findById(DCB_TRANSACTION_ID)).thenReturn(Optional.of(dcbTransactionEntity));
    when(statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT))
      .thenReturn(List.of(TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT));
    doNothing().when(lendingLibraryService).updateTransactionStatus(dcbTransactionEntity, TransactionStatus.builder().status(TransactionStatus.StatusEnum.AWAITING_PICKUP).build());
    doNothing().when(lendingLibraryService).updateTransactionStatus(dcbTransactionEntity, TransactionStatus.builder().status(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT).build());

    transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID, TransactionStatus.builder().status(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT).build());

    verify(statusProcessorService).lendingChainProcessor(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
  }

  @Test
  void updateTransactionEntityErrorTest() {
    var dcbTransactionEntity = createTransactionEntity();
    dcbTransactionEntity.setStatus(TransactionStatus.StatusEnum.OPEN);
    when(transactionRepository.findById(DCB_TRANSACTION_ID)).thenReturn(Optional.of(dcbTransactionEntity));
    var openTransactionStatus = TransactionStatus.builder().status(TransactionStatus.StatusEnum.OPEN).build();

    Assertions.assertThrows(StatusException.class, () -> transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID, openTransactionStatus));

    dcbTransactionEntity.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    var cancelledTransactionStatus = TransactionStatus.builder().status(TransactionStatus.StatusEnum.CANCELLED).build();
    Assertions.assertThrows(StatusException.class, () -> transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID, cancelledTransactionStatus));

    dcbTransactionEntity.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    Assertions.assertThrows(StatusException.class, () -> transactionsService.updateTransactionStatus(DCB_TRANSACTION_ID, cancelledTransactionStatus));
  }

}
