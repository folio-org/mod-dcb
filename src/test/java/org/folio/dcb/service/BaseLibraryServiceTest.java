package org.folio.dcb.service;

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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.folio.dcb.exception.CirculationRequestException;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ERROR;
import static org.mockito.Mockito.doThrow;
import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.DcbUpdateItem;
import org.folio.dcb.domain.dto.User;
import org.mockito.ArgumentCaptor;
import java.util.Collections;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class BaseLibraryServiceTest {

  @InjectMocks private BaseLibraryService baseLibraryService;
  @Mock private TransactionRepository transactionRepository;
  @Mock private UserService userService;
  @Mock private RequestService requestService;
  @Mock private CirculationItemService circulationItemService;
  @Mock private CirculationService circulationService;
  @Mock private TransactionMapper transactionMapper;
  @Mock private ItemService itemService;

  @Test
  void updateTransactionWithWrongStatusTest() {
    TransactionEntity transactionEntity = createTransactionEntity();
    TransactionStatus transactionStatus = createTransactionStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    assertThrows(IllegalArgumentException.class, () ->
      baseLibraryService.updateTransactionStatus(transactionEntity, transactionStatus));
  }

  @Test
  void updateTransactionErrorTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(BORROWING_PICKUP);
    var transactionStatus = TransactionStatus.builder()
      .status(TransactionStatus.StatusEnum.AWAITING_PICKUP)
      .build();
    assertThrows(IllegalArgumentException.class, () ->
      baseLibraryService.updateTransactionStatus(transactionEntity, transactionStatus));
  }

  @Test
  void updateTransactionStatusFromItemCheckedInToClosedTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    transactionEntity.setRole(BORROWING_PICKUP);

    baseLibraryService.updateTransactionStatus(transactionEntity, TransactionStatus.builder().status(CLOSED).build());

    assertEquals(CLOSED, transactionEntity.getStatus());
  }

  @Test
  void updateTransactionTestFromCreatedToOpen() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(BORROWING_PICKUP);
    doNothing().when(circulationService).checkInByBarcode(any(), any());

    baseLibraryService.updateTransactionStatus(
      transactionEntity, TransactionStatus.builder().status(TransactionStatus.StatusEnum.OPEN).build());
    verify(transactionRepository, times(1)).save(transactionEntity);
    verify(circulationService, timeout(1)).checkInByBarcode(any(), any());
  }

  @Test
  void createBorrowingTransactionTest() {
    var user = createUser();
    user.setType("shadow");
    var circulationItem = createCirculationItem();
    var item = createDcbItem();

    when(userService.fetchUser(any()))
      .thenReturn(user);
    when(requestService.createHoldItemRequest(any(), any(), anyString())).thenReturn(createCirculationRequest());
    when(transactionMapper.mapToEntity(any(), any())).thenReturn(createTransactionEntity());
    when(itemService.fetchItemByBarcode(item.getBarcode())).thenReturn(new ResultList<>());
    when(circulationItemService.checkIfItemExistsAndCreate(any(), any())).thenReturn(circulationItem);

    var response = baseLibraryService.createBorrowingLibraryTransaction(
      DCB_TRANSACTION_ID, createDcbTransactionByRole(BORROWER), PICKUP_SERVICE_POINT_ID);

    assertEquals(TransactionStatusResponse.StatusEnum.CREATED, response.getStatus());
    verify(userService).fetchUser(createDcbPatronWithExactPatronId(EXISTED_PATRON_ID));
    // Circulation item id will be set as dcb item id in the code, hence setting it for assertion
    item.setId(circulationItem.getId());
    verify(requestService).createHoldItemRequest(user, item, PICKUP_SERVICE_POINT_ID);
    verify(transactionRepository).save(any());
  }

  @Test
  void createBorrowingPickupWithSelfBorrowingTransactionTest() {
    var user = createUser();
    user.setType("staff");
    var dcbTransaction = createDcbTransactionByRoleAndSelfBorrowing(BORROWING_PICKUP, Boolean.TRUE);

    when(userService.fetchUser(any())).thenReturn(user);
    when(transactionMapper.mapToEntity(any(), any())).thenReturn(createTransactionEntity());
    when(requestService.createRequestBasedOnItemStatus(any(), any(), anyString()))
      .thenReturn(createCirculationRequest());

    var response =
      baseLibraryService.createBorrowingLibraryTransaction(DCB_TRANSACTION_ID, dcbTransaction, PICKUP_SERVICE_POINT_ID);

    var item = createDcbItem();
    var patron = createDcbPatronWithExactPatronId(EXISTED_PATRON_ID);
    assertEquals(TransactionStatusResponse.StatusEnum.CREATED, response.getStatus());
    verify(requestService).createRequestBasedOnItemStatus(user, item, dcbTransaction.getPickup().getServicePointId());
    verify(userService).fetchUser(patron);
    verify(circulationItemService, never()).checkIfItemExistsAndCreate(any(), any());
    verify(transactionRepository).save(any());

    var servicePoint = createServicePointRequest();
    assertEquals(servicePoint.getId(), dcbTransaction.getPickup().getServicePointId());
  }

  @Test
  void createDuplicateBorrowingTransactionTest() {
    var item = createDcbItem();
    var user = createUser();
    user.setType("shadow");

    when(userService.fetchUser(any())).thenReturn(user);
    when(itemService.fetchItemByBarcode(item.getBarcode())).thenReturn(new ResultList<>());
    when(circulationItemService.checkIfItemExistsAndCreate(any(), any())).thenReturn(createCirculationItem());
    when(transactionRepository.findTransactionsByItemIdAndStatusNotInClosed(any()))
      .thenReturn(List.of(createTransactionEntity()));
    
    var transaction = createDcbTransactionByRole(BORROWER);
    assertThrows(ResourceAlreadyExistException.class, () ->
      baseLibraryService.createBorrowingLibraryTransaction(
        DCB_TRANSACTION_ID, transaction, PICKUP_SERVICE_POINT_ID));
  }

  @Test
  void createBorrowingTransaction_positive_expiredTransactionExistsForSameItem() {
    var user = createUser();
    user.setType("shadow");

    when(userService.fetchUser(any())).thenReturn(user);
    when(requestService.createHoldItemRequest(any(), any(), anyString()))
        .thenReturn(createCirculationRequest());
    when(transactionMapper.mapToEntity(any(), any())).thenReturn(createTransactionEntity());
    when(itemService.fetchItemByBarcode(createDcbItem().getBarcode())).thenReturn(new ResultList<>());
    when(circulationItemService.checkIfItemExistsAndCreate(any(), any()))
        .thenReturn(createCirculationItem());
    when(transactionRepository.findTransactionsByItemIdAndStatusNotInClosed(any()))
        .thenReturn(List.of());

    var dcbTransaction = createDcbTransactionByRole(BORROWER);
    var response = baseLibraryService.createBorrowingLibraryTransaction(
      DCB_TRANSACTION_ID, dcbTransaction, PICKUP_SERVICE_POINT_ID);
    verify(transactionRepository).save(any());
    assertEquals(TransactionStatusResponse.StatusEnum.CREATED, response.getStatus());
  }

  @Test
  void checkItemIfExistsInInventory() {
    var item = createDcbItem();
    var inventoryItem = createInventoryItem();

    when(itemService.fetchItemByBarcode(item.getBarcode()))
        .thenReturn(ResultList.of(1, List.of(inventoryItem)));

    assertThrows(ResourceAlreadyExistException.class, () ->
      baseLibraryService.checkItemExistsInInventoryAndThrow("DCB_ITEM"));
  }

  @Test
  void checkItemIfNotExistsInInventory() {
    var item = createDcbItem();

    when(itemService.fetchItemByBarcode(item.getBarcode())).thenReturn(ResultList.empty());

    baseLibraryService.checkItemExistsInInventoryAndThrow(item.getBarcode());
    verify(itemService).fetchItemByBarcode(item.getBarcode());
  }

  @Test
  void createBorrowingTransactionTestThrowException() {
    var user = createUser();
    var transaction = createDcbTransactionByRole(BORROWER);
    when(userService.fetchUser(any())).thenReturn(user);

    assertThrows(IllegalArgumentException.class, () ->
      baseLibraryService.createBorrowingLibraryTransaction(DCB_TRANSACTION_ID, transaction, PICKUP_SERVICE_POINT_ID));
  }

  @Test
  void testTransactionCancelTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(OPEN);
    TransactionStatus transactionStatus = TransactionStatus.builder().status(CANCELLED).build();
    baseLibraryService.updateTransactionStatus(transactionEntity, transactionStatus);
    verify(circulationService).cancelRequest(any(), eq(false));
  }

  @Test
  void saveTransactionTest() {
    when(transactionMapper.mapToEntity(any(), any())).thenReturn(createTransactionEntity());

    baseLibraryService.saveDcbTransaction(
        DCB_TRANSACTION_ID, createDcbTransactionByRole(BORROWER), CIRCULATION_REQUEST_ID);
    verify(transactionRepository).save(any());
  }

    @Test
  void cancelTransactionRequest_negative_shouldUpdateStatusToErrorWhenCirculationFails() {
    // TestMate-4fb4669c771be6fc64a27fa97c03a18e
    // Given
    TransactionEntity transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(OPEN);
    doThrow(new CirculationRequestException("Circulation failure"))
      .when(circulationService).cancelRequest(any(TransactionEntity.class), eq(false));
    // When
    baseLibraryService.cancelTransactionRequest(transactionEntity);
    // Then
    assertEquals(ERROR, transactionEntity.getStatus());
    verify(transactionRepository).save(transactionEntity);
  }

    @Test
  void updateTransactionDetailsShouldSuccessfullyUpdateItemAndRequest() {
    // TestMate-4b7b31d38c5fbbad70aee8e7eecd953b
    // Given
    String newBarcode = "NEW-ITEM-001";
    String newMaterialType = "book";
    String newLendingLibraryCode = "LEND_001";
    String newCirculationItemId = "f5a28723-144a-463d-8f24-91221764722d";
    String newRequestId = "d290f1ee-6c54-4b01-90e6-d701748f0851";
    TransactionEntity transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(OPEN);
    DcbUpdateItem dcbUpdateItem = DcbUpdateItem.builder()
      .barcode(newBarcode)
      .materialType(newMaterialType)
      .lendingLibraryCode(newLendingLibraryCode)
      .build();
    DcbItem dcbItem = createDcbItem();
    dcbItem.setBarcode(newBarcode);
    dcbItem.setMaterialType(newMaterialType);
    dcbItem.setLendingLibraryCode(newLendingLibraryCode);
    CirculationItem circulationItem = createCirculationItem();
    circulationItem.setId(newCirculationItemId);
    circulationItem.setBarcode(newBarcode);
    circulationItem.setLendingLibraryCode(newLendingLibraryCode);
    DcbPatron dcbPatron = createDcbPatronWithExactPatronId(EXISTED_PATRON_ID);
    User user = createUser();
    CirculationRequest holdRequest = createCirculationRequest();
    holdRequest.setId(newRequestId);
    when(transactionMapper.convertTransactionUpdateItemToDcbItem(dcbUpdateItem, transactionEntity)).thenReturn(dcbItem);
    when(itemService.fetchItemByBarcode(newBarcode)).thenReturn(new ResultList<>());
    when(circulationItemService.checkIfItemExistsAndCreate(dcbItem, transactionEntity.getServicePointId())).thenReturn(circulationItem);
    when(transactionRepository.findTransactionsByItemIdAndStatusNotInClosed(UUID.fromString(newCirculationItemId))).thenReturn(Collections.emptyList());
    when(transactionMapper.mapTransactionEntityToDcbPatron(transactionEntity)).thenReturn(dcbPatron);
    when(userService.fetchUser(dcbPatron)).thenReturn(user);
    when(requestService.createHoldItemRequest(user, dcbItem, transactionEntity.getServicePointId())).thenReturn(holdRequest);
    // When
    baseLibraryService.updateTransactionDetails(transactionEntity, dcbUpdateItem);
    // Then
    verify(circulationService).cancelRequest(transactionEntity, true);
    verify(requestService).createHoldItemRequest(user, dcbItem, transactionEntity.getServicePointId());
    ArgumentCaptor<TransactionEntity> entityCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
    verify(transactionRepository).save(entityCaptor.capture());
    TransactionEntity savedEntity = entityCaptor.getValue();
    assertThat(savedEntity.getItemId()).isEqualTo(newCirculationItemId);
    assertThat(savedEntity.getItemBarcode()).isEqualTo(newBarcode);
    assertThat(savedEntity.getRequestId()).isEqualTo(UUID.fromString(newRequestId));
    assertThat(savedEntity.getLendingLibraryCode()).isEqualTo(newLendingLibraryCode);
    assertThat(savedEntity.getMaterialType()).isEqualTo(newMaterialType);
    assertThat(savedEntity.getStatus()).isEqualTo(CREATED);
  }

    @Test
  void updateTransactionDetailsShouldThrowExceptionWhenItemHasOpenTransaction() {
    // TestMate-9f8b4f7597c8691c3441b70fea5c7a10
    // Given
    String newBarcode = "NEW-ITEM-BARCODE";
    UUID newItemId = UUID.fromString("00000000-1111-2222-3333-444444444444");
    TransactionEntity transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(OPEN);
    
    DcbUpdateItem dcbUpdateItem = DcbUpdateItem.builder()
      .barcode(newBarcode)
      .materialType("book")
      .lendingLibraryCode("LEND_001")
      .build();
    DcbItem dcbItem = createDcbItem();
    dcbItem.setBarcode(newBarcode);
    CirculationItem circulationItem = createCirculationItem();
    circulationItem.setId(newItemId.toString());
    when(transactionMapper.convertTransactionUpdateItemToDcbItem(dcbUpdateItem, transactionEntity)).thenReturn(dcbItem);
    when(itemService.fetchItemByBarcode(newBarcode)).thenReturn(new ResultList<>());
    when(circulationItemService.checkIfItemExistsAndCreate(dcbItem, transactionEntity.getServicePointId())).thenReturn(circulationItem);
    when(transactionRepository.findTransactionsByItemIdAndStatusNotInClosed(newItemId)).thenReturn(List.of(createTransactionEntity()));
    // When
    assertThatThrownBy(() -> baseLibraryService.updateTransactionDetails(transactionEntity, dcbUpdateItem))
      .isInstanceOf(ResourceAlreadyExistException.class)
      .hasMessageContaining(String.format("Item with id %s already has an open DCB transaction", newItemId));
    // Then
    verify(circulationService, never()).cancelRequest(any(), any(Boolean.class));
    verify(requestService, never()).createHoldItemRequest(any(), any(), any());
    verify(transactionRepository, never()).save(any());
  }
}
