package org.folio.dcb.service;

import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.BorrowingLibraryServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.AWAITING_PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BorrowingLibraryServiceTest {
  @InjectMocks
  private BorrowingLibraryServiceImpl borrowingLibraryService;
  @Mock
  private CirculationService circulationService;
  @Mock
  private TransactionRepository transactionRepository;

  @Test
  void testTransactionStatusUpdateFromOpenToAwaitingPickup() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(OPEN);
    doNothing().when(circulationService).checkInByBarcode(transactionEntity);
    TransactionStatus transactionStatus = TransactionStatus.builder().status(AWAITING_PICKUP).build();
    borrowingLibraryService.updateTransactionStatus(transactionEntity, transactionStatus);

    verify(circulationService).checkInByBarcode(any());
    Assertions.assertEquals(AWAITING_PICKUP, transactionEntity.getStatus());
  }

  @Test
  void testTransactionStatusUpdateWithIncorrectStatus() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(CREATED);
    TransactionStatus transactionStatus = TransactionStatus.builder().status(AWAITING_PICKUP).build();
    assertThrows(IllegalArgumentException.class, () -> borrowingLibraryService.updateTransactionStatus(transactionEntity, transactionStatus));
  }

}
