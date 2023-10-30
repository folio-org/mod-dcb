package org.folio.dcb.service;

import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.BorrowingLibraryServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWER;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowingLibraryServiceTest {
  @InjectMocks
  private BorrowingLibraryServiceImpl borrowingLibraryService;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private CirculationService circulationService;
  @Mock
  private CirculationClient circulationClient;

  @Test
  void updateTransactionTestFromCreatedToOpen() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(BORROWER);
    doNothing().when(circulationService).checkInByBarcode(any(), any());

    borrowingLibraryService.updateTransactionStatus(transactionEntity, TransactionStatus.builder().status(TransactionStatus.StatusEnum.OPEN).build());
    verify(transactionRepository, times(1)).save(transactionEntity);
    verify(circulationService, timeout(1)).checkInByBarcode(any(), any());
  }

  @Test
  void updateTransactionErrorTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(BORROWER);
    TransactionStatus transactionStatus = TransactionStatus.builder().status(TransactionStatus.StatusEnum.AWAITING_PICKUP).build();
    assertThrows(IllegalArgumentException.class, () -> borrowingLibraryService.updateTransactionStatus(transactionEntity, transactionStatus));
  }

  @Test
  void updateTransactionStatusFromItemCheckedInToClosedTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    transactionEntity.setRole(BORROWER);

    borrowingLibraryService.updateTransactionStatus(transactionEntity, TransactionStatus.builder().status(CLOSED).build());

    Assertions.assertEquals(CLOSED, transactionEntity.getStatus());
  }
}
