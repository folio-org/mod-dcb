
package org.folio.dcb.service;

import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CANCELLED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;
import static org.folio.dcb.utils.EntityUtils.CIRCULATION_REQUEST_ID;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.PICKUP_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.createCirculationItem;
import static org.folio.dcb.utils.EntityUtils.createCirculationRequest;
import static org.folio.dcb.utils.EntityUtils.createDcbItem;
import static org.folio.dcb.utils.EntityUtils.createDcbPatronWithExactPatronId;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRoleAndSelfBorrowing;
import static org.folio.dcb.utils.EntityUtils.createInventoryItem;
import static org.folio.dcb.utils.EntityUtils.createServicePointRequest;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.createTransactionStatus;
import static org.folio.dcb.utils.EntityUtils.createUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BaseLibraryServiceTest {

  @InjectMocks
  private BaseLibraryService baseLibraryService;
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
  @Mock
  private TransactionMapper transactionMapper;
  @Mock
  private ItemService itemService;

  @Test
  void updateTransactionWithWrongStatusTest() {
    TransactionEntity transactionEntity = createTransactionEntity();
    TransactionStatus transactionStatus = createTransactionStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    assertThrows(IllegalArgumentException.class, () -> baseLibraryService.updateTransactionStatus(transactionEntity, transactionStatus));
  }

  @Test
  void updateTransactionErrorTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(BORROWING_PICKUP);
    TransactionStatus transactionStatus = TransactionStatus.builder().status(TransactionStatus.StatusEnum.AWAITING_PICKUP).build();
    assertThrows(IllegalArgumentException.class, () -> baseLibraryService.updateTransactionStatus(transactionEntity, transactionStatus));
  }

  @Test
  void updateTransactionStatusFromItemCheckedInToClosedTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    transactionEntity.setRole(BORROWING_PICKUP);

    baseLibraryService.updateTransactionStatus(transactionEntity, TransactionStatus.builder().status(CLOSED).build());

    Assertions.assertEquals(CLOSED, transactionEntity.getStatus());
  }

  @Test
  void updateTransactionTestFromCreatedToOpen() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(BORROWING_PICKUP);
    doNothing().when(circulationService).checkInByBarcode(any(), any());

    baseLibraryService.updateTransactionStatus(transactionEntity, TransactionStatus.builder().status(TransactionStatus.StatusEnum.OPEN).build());
    verify(transactionRepository, times(1)).save(transactionEntity);
    verify(circulationService, timeout(1)).checkInByBarcode(any(), any());
  }

  @Test
  void createBorrowingTransactionTest() {
    var item = createDcbItem();
    var patron = createDcbPatronWithExactPatronId(EXISTED_PATRON_ID);
    var user = createUser();
    user.setType("shadow");
    var circulationItem = createCirculationItem();

    when(userService.fetchUser(any()))
      .thenReturn(user);
    when(requestService.createHoldItemRequest(any(), any(), anyString())).thenReturn(createCirculationRequest());
    when(transactionMapper.mapToEntity(any(), any())).thenReturn(createTransactionEntity());
    when(itemService.fetchItemByBarcode(item.getBarcode())).thenReturn(new ResultList<>());
    when(circulationItemService.checkIfItemExistsAndCreate(any(), any())).thenReturn(circulationItem);
    var response = baseLibraryService.createBorrowingLibraryTransaction(DCB_TRANSACTION_ID, createDcbTransactionByRole(BORROWER), PICKUP_SERVICE_POINT_ID);
    verify(userService).fetchUser(patron);
    // Circulation item id will be set as dcb item id in the code, hence setting it for assertion
    item.setId(circulationItem.getId());
    verify(requestService).createHoldItemRequest(user, item, PICKUP_SERVICE_POINT_ID);
    verify(transactionRepository).save(any());
    Assertions.assertEquals(TransactionStatusResponse.StatusEnum.CREATED, response.getStatus());
  }

  @Test
  void createBorrowingPickupWithSelfBorrowingTransactionTest() {
    var item = createDcbItem();
    var patron = createDcbPatronWithExactPatronId(EXISTED_PATRON_ID);
    var user = createUser();
    user.setType("staff");
    var dcbTransaction = createDcbTransactionByRoleAndSelfBorrowing(BORROWING_PICKUP, Boolean.TRUE);
    var servicePoint = createServicePointRequest();


    when(userService.fetchUser(any())).thenReturn(user);
    when(transactionMapper.mapToEntity(any(), any())).thenReturn(createTransactionEntity());
    when(requestService.createRequestBasedOnItemStatus(any(), any(), anyString())).thenReturn(createCirculationRequest());

    var response = baseLibraryService.createBorrowingLibraryTransaction(DCB_TRANSACTION_ID, dcbTransaction, PICKUP_SERVICE_POINT_ID);
    verify(userService).fetchUser(patron);
    verify(circulationItemService, never()).checkIfItemExistsAndCreate(any(), any());
    verify(requestService).createRequestBasedOnItemStatus(user, item, dcbTransaction.getPickup().getServicePointId());
    verify(transactionRepository).save(any());

    Assertions.assertEquals(TransactionStatusResponse.StatusEnum.CREATED, response.getStatus());
    assertEquals(servicePoint.getId(), dcbTransaction.getPickup().getServicePointId());
  }

  @Test
  void createDuplicateBorrowingTransactionTest() {
    var item = createDcbItem();
    var user = createUser();
    user.setType("shadow");

    when(userService.fetchUser(any()))
      .thenReturn(user);
    when(itemService.fetchItemByBarcode(item.getBarcode())).thenReturn(new ResultList<>());
    when(circulationItemService.checkIfItemExistsAndCreate(any(), any())).thenReturn(createCirculationItem());
    when(transactionRepository.findTransactionsByItemIdAndStatusNotInClosed(any())).thenReturn(List.of(createTransactionEntity()));
    assertThrows(ResourceAlreadyExistException.class, () -> baseLibraryService.createBorrowingLibraryTransaction(DCB_TRANSACTION_ID, createDcbTransactionByRole(BORROWER), PICKUP_SERVICE_POINT_ID));
  }

  @Test
  void checkItemIfExistsInInventory() {
    var item = createDcbItem();
    var inventoryItem = createInventoryItem();

    when(itemService.fetchItemByBarcode(item.getBarcode())).thenReturn(ResultList.of(1, List.of(inventoryItem)));

    assertThrows(ResourceAlreadyExistException.class, () -> baseLibraryService.checkItemExistsInInventoryAndThrow("DCB_ITEM"));
  }

  @Test
  void checkItemIfNotExistsInInventory() {
    var item = createDcbItem();

    when(itemService.fetchItemByBarcode(item.getBarcode())).thenReturn(ResultList.of(0, List.of()));

    baseLibraryService.checkItemExistsInInventoryAndThrow(item.getBarcode());
    verify(itemService).fetchItemByBarcode(item.getBarcode());
  }

  @Test
  void createBorrowingTransactionTestThrowException() {
    var user = createUser();
    var transaction = createDcbTransactionByRole(BORROWER);
    when(userService.fetchUser(any()))
      .thenReturn(user);

    assertThrows(IllegalArgumentException.class, () -> baseLibraryService.createBorrowingLibraryTransaction(DCB_TRANSACTION_ID, transaction, PICKUP_SERVICE_POINT_ID));
  }

  @Test
  void testTransactionCancelTest(){
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(OPEN);
    TransactionStatus transactionStatus = TransactionStatus.builder().status(CANCELLED).build();
    baseLibraryService.updateTransactionStatus(transactionEntity, transactionStatus);
    verify(circulationService).cancelRequest(any(), eq(false));
  }

  @Test
  void saveTransactionTest() {
    when(transactionMapper.mapToEntity(any(), any())).thenReturn(createTransactionEntity());

    baseLibraryService.saveDcbTransaction(DCB_TRANSACTION_ID, createDcbTransactionByRole(BORROWER), CIRCULATION_REQUEST_ID);
    verify(transactionRepository).save(any());
  }

}
