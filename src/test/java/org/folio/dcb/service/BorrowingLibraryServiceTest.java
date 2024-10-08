package org.folio.dcb.service;

import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.folio.dcb.service.impl.BorrowingLibraryServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWER;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.AWAITING_PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_IN;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_OUT;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.createDcbPickup;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createServicePointRequest;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class BorrowingLibraryServiceTest {
  @InjectMocks
  private BorrowingLibraryServiceImpl borrowingLibraryService;
  @Mock
  private CirculationService circulationService;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private BaseLibraryService baseLibraryService;

  @Mock
  private ServicePointService servicePointService;


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
  void testTransactionStatusUpdateFromCreatedToOpen() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(CREATED);
    doNothing().when(circulationService).checkInByBarcode(any(), any());
    TransactionStatus transactionStatus = TransactionStatus.builder().status(OPEN).build();
    borrowingLibraryService.updateTransactionStatus(transactionEntity, transactionStatus);

    verify(circulationService).checkInByBarcode(any(), any());
    Assertions.assertEquals(OPEN, transactionEntity.getStatus());
  }

  @Test
  void testTransactionStatusUpdateWithIncorrectStatus() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(CREATED);
    TransactionStatus transactionStatus = TransactionStatus.builder().status(AWAITING_PICKUP).build();
    assertThrows(IllegalArgumentException.class, () -> borrowingLibraryService.updateTransactionStatus(transactionEntity, transactionStatus));
  }

  @Test
  void createTransactionTest() {
    var servicePointRequest = createServicePointRequest();
    var dcbTransaction = createDcbTransactionByRole(BORROWER);
    servicePointRequest.setId(UUID.randomUUID().toString());
    when(servicePointService.createServicePointIfNotExists(createDcbPickup())).thenReturn(servicePointRequest);
    borrowingLibraryService.createCirculation(DCB_TRANSACTION_ID, dcbTransaction);
    assertEquals(servicePointRequest.getId(), dcbTransaction.getPickup().getServicePointId());
    verify(baseLibraryService).createBorrowingLibraryTransaction(DCB_TRANSACTION_ID, dcbTransaction, servicePointRequest.getId());
  }

  @Test
  void testTransactionStatusUpdateFromItemCheckedInToClosed() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(ITEM_CHECKED_IN);
    TransactionStatus transactionStatus = TransactionStatus.builder().status(CLOSED).build();
    borrowingLibraryService.updateTransactionStatus(transactionEntity, transactionStatus);

    Assertions.assertEquals(CLOSED, transactionEntity.getStatus());
  }

  @Test
  void testTransactionStatusUpdateFromAwaitingPickupToItemCheckedOut() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(AWAITING_PICKUP);
    doNothing().when(circulationService).checkOutByBarcode(transactionEntity);
    TransactionStatus transactionStatus = TransactionStatus.builder().status(ITEM_CHECKED_OUT).build();
    borrowingLibraryService.updateTransactionStatus(transactionEntity, transactionStatus);

    verify(circulationService).checkOutByBarcode(any());
    Assertions.assertEquals(ITEM_CHECKED_OUT, transactionEntity.getStatus());
  }

  @Test
  void testTransactionStatusUpdateFromItemCheckedOutToItemCheckedIn() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(ITEM_CHECKED_OUT);
    doNothing().when(circulationService).checkInByBarcode(any(), any());
    TransactionStatus transactionStatus = TransactionStatus.builder().status(ITEM_CHECKED_IN).build();
    borrowingLibraryService.updateTransactionStatus(transactionEntity, transactionStatus);

    verify(circulationService).checkInByBarcode(any(), any());
    Assertions.assertEquals(ITEM_CHECKED_IN, transactionEntity.getStatus());
  }

}
