package org.folio.dcb.service;

import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.folio.dcb.service.impl.CirculationServiceImpl;
import org.folio.dcb.service.impl.LendingLibraryServiceImpl;
import org.folio.dcb.service.impl.RequestServiceImpl;
import org.folio.dcb.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.PICKUP_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.createDcbItem;
import static org.folio.dcb.utils.EntityUtils.createDefaultDcbPatron;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.createTransactionStatus;
import static org.folio.dcb.utils.EntityUtils.createUser;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LendingLibraryServiceTest {

  @InjectMocks
  private LendingLibraryServiceImpl lendingLibraryService;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private UserServiceImpl userService;
  @Mock
  private RequestServiceImpl requestService;

  @Mock
  private CirculationServiceImpl circulationService;
  @Mock
  private BaseLibraryService baseLibraryService;

  @Test
  void createTransactionTest() {
    var item = createDcbItem();
    var patron = createDefaultDcbPatron();
    var user = createUser();

    when(userService.fetchOrCreateUser(any()))
      .thenReturn(user);
    doNothing().when(requestService).createPageItemRequest(any(), any(), any());
    doNothing().when(baseLibraryService).saveDcbTransaction(any(), any());

    var response = lendingLibraryService.createCirculation(DCB_TRANSACTION_ID, createDcbTransactionByRole(LENDER), PICKUP_SERVICE_POINT_ID);
    verify(userService).fetchOrCreateUser(patron);
    verify(requestService).createPageItemRequest(user, item, PICKUP_SERVICE_POINT_ID);
    verify(baseLibraryService).saveDcbTransaction(DCB_TRANSACTION_ID, createDcbTransactionByRole(LENDER));
    Assertions.assertEquals(TransactionStatusResponse.StatusEnum.CREATED, response.getStatus());
  }

  @Test
  void updateTransactionTestFromCreatedToOpen() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(LENDER);

    lendingLibraryService.updateStatusByTransactionEntity(transactionEntity);
    Mockito.verify(transactionRepository, times(1)).save(transactionEntity);
  }

  @Test
  void updateTransactionTestFromCheckInToClose() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    transactionEntity.setRole(LENDER);

    lendingLibraryService.updateStatusByTransactionEntity(transactionEntity);
    Mockito.verify(transactionRepository, times(1)).save(transactionEntity);
  }

  @Test
  void updateTransactionTestWithInvalidData() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(LENDER);
    assertDoesNotThrow(() -> lendingLibraryService.updateStatusByTransactionEntity(transactionEntity));
  }

  @Test
  void transactionStatusFromOpenToAwaitingTest() {
    TransactionEntity dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.OPEN);
    doNothing().when(circulationService).checkInByBarcode(dcbTransaction);
    lendingLibraryService.updateTransactionStatus(dcbTransaction, TransactionStatus.builder().status(TransactionStatus.StatusEnum.AWAITING_PICKUP).build());

    verify(circulationService).checkInByBarcode(dcbTransaction);

    Assertions.assertEquals(TransactionStatus.StatusEnum.AWAITING_PICKUP, dcbTransaction.getStatus());
  }

  @Test
  void transactionStatusFromAwaitingToCheckoutTest() {
    TransactionEntity dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    doNothing().when(circulationService).checkOutByBarcode(dcbTransaction);
    lendingLibraryService.updateTransactionStatus(dcbTransaction, TransactionStatus.builder().status(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT).build());

    verify(circulationService).checkOutByBarcode(dcbTransaction);

    Assertions.assertEquals(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT, dcbTransaction.getStatus());
  }

  @Test
  void transactionStatusFromCheckoutToCheckInTest() {
    TransactionEntity dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    lendingLibraryService.updateTransactionStatus(dcbTransaction, TransactionStatus.builder().status(TransactionStatus.StatusEnum.ITEM_CHECKED_IN).build());

    Assertions.assertEquals(TransactionStatus.StatusEnum.ITEM_CHECKED_IN, dcbTransaction.getStatus());
  }

  @Test
  void updateTransactionWithWrongStatusTest() {
    TransactionEntity transactionEntity = createTransactionEntity();
    TransactionStatus transactionStatus = createTransactionStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    assertThrows(IllegalArgumentException.class, () -> lendingLibraryService.updateTransactionStatus(transactionEntity, transactionStatus));
  }
}
