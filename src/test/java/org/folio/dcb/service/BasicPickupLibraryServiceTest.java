package org.folio.dcb.service;

import org.folio.dcb.domain.dto.CirculationItemRequest;
import org.folio.dcb.domain.dto.ItemStatus;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.BasicPickupLibraryServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.createTransactionStatus;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BasicPickupLibraryServiceTest {

  @InjectMocks
  private BasicPickupLibraryServiceImpl basicPickupLibraryService;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private UserService userService;
  @Mock
  private RequestService requestService;
  @Mock
  private CirculationItemService circulationItemService;
  @Mock
  private CirculationService circulationService;

  @Test
  void updateTransactionWithWrongStatusTest() {
    TransactionEntity transactionEntity = createTransactionEntity();
    TransactionStatus transactionStatus = createTransactionStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    assertThrows(IllegalArgumentException.class, () -> basicPickupLibraryService.updateTransactionStatus(transactionEntity, transactionStatus));
  }

  @Test
  void updateTransactionErrorTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(BORROWING_PICKUP);
    TransactionStatus transactionStatus = TransactionStatus.builder().status(TransactionStatus.StatusEnum.AWAITING_PICKUP).build();
    assertThrows(IllegalArgumentException.class, () -> basicPickupLibraryService.updateTransactionStatus(transactionEntity, transactionStatus));
  }

  @Test
  void updateTransactionStatusFromItemCheckedInToClosedTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    transactionEntity.setRole(BORROWING_PICKUP);

    basicPickupLibraryService.updateTransactionStatus(transactionEntity, TransactionStatus.builder().status(CLOSED).build());

    Assertions.assertEquals(CLOSED, transactionEntity.getStatus());
  }

  @Test
  void updateTransactionTestFromCheckedOutToCheckedIns() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    transactionEntity.setRole(BORROWING_PICKUP);
    when(circulationItemService.fetchItemById(any())).thenReturn(CirculationItemRequest.builder().status(
      ItemStatus.builder().name(ItemStatus.NameEnum.AVAILABLE).build()).build());
    basicPickupLibraryService.updateStatusByTransactionEntity(transactionEntity);
    Mockito.verify(transactionRepository, times(1)).save(transactionEntity);
  }

  @Test
  void updateTransactionTestFromCreatedToOpen() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(BORROWING_PICKUP);
    doNothing().when(circulationService).checkInByBarcode(any(), any());

    basicPickupLibraryService.updateTransactionStatus(transactionEntity, TransactionStatus.builder().status(TransactionStatus.StatusEnum.OPEN).build());
    verify(transactionRepository, times(1)).save(transactionEntity);
    verify(circulationService, timeout(1)).checkInByBarcode(any(), any());
  }
}
