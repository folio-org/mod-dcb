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
import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.DcbUpdateItem;
import org.folio.dcb.domain.dto.User;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.CirculationService;
import org.folio.dcb.service.ItemService;
import org.folio.dcb.service.RequestService;
import org.folio.dcb.service.UserService;
import java.util.Collections;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.folio.dcb.domain.dto.InventoryItem;

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
  void updateTransactionDetailsShouldSuccessfullyUpdateTransactionWhenItemIsNew() {
    // TestMate-011ae528e6a6a0ab34dcf77452354d97
    // Given
    String newBarcode = "new-item-barcode";
    String newLendingLibraryCode = "NEW_LENDER";
    String materialType = "book";
    UUID newCirculationItemId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID newRequestId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    UUID patronId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    TransactionEntity transactionEntity = createTransactionEntity();
    transactionEntity.setServicePointId(PICKUP_SERVICE_POINT_ID);
    transactionEntity.setPatronId(patronId.toString());
    DcbUpdateItem dcbUpdateItem = DcbUpdateItem.builder()
      .barcode(newBarcode)
      .lendingLibraryCode(newLendingLibraryCode)
      .materialType(materialType)
      .build();
    DcbItem dcbItem = DcbItem.builder()
      .barcode(newBarcode)
      .lendingLibraryCode(newLendingLibraryCode)
      .materialType(materialType)
      .title(transactionEntity.getItemTitle())
      .build();
    CirculationItem circulationItem = CirculationItem.builder()
      .id(newCirculationItemId.toString())
      .barcode(newBarcode)
      .lendingLibraryCode(newLendingLibraryCode)
      .build();
    User user = User.builder()
      .id(patronId.toString())
      .type("shadow")
      .build();
    DcbPatron dcbPatron = DcbPatron.builder()
      .id(patronId.toString())
      .build();
    CirculationRequest holdRequest = CirculationRequest.builder()
      .id(newRequestId.toString())
      .build();
    when(transactionMapper.convertTransactionUpdateItemToDcbItem(dcbUpdateItem, transactionEntity)).thenReturn(dcbItem);
    when(itemService.fetchItemByBarcode(newBarcode)).thenReturn(new ResultList<>());
    when(circulationItemService.checkIfItemExistsAndCreate(dcbItem, PICKUP_SERVICE_POINT_ID)).thenReturn(circulationItem);
    when(transactionRepository.findTransactionsByItemIdAndStatusNotInClosed(newCirculationItemId)).thenReturn(Collections.emptyList());
    when(transactionMapper.mapTransactionEntityToDcbPatron(transactionEntity)).thenReturn(dcbPatron);
    when(userService.fetchUser(dcbPatron)).thenReturn(user);
    when(requestService.createHoldItemRequest(user, dcbItem, PICKUP_SERVICE_POINT_ID)).thenReturn(holdRequest);
    // When
    baseLibraryService.updateTransactionDetails(transactionEntity, dcbUpdateItem);
    // Then
    verify(circulationService).cancelRequest(transactionEntity, true);
    verify(requestService).createHoldItemRequest(user, dcbItem, PICKUP_SERVICE_POINT_ID);
    verify(transactionRepository).save(transactionEntity);
    assertThat(transactionEntity.getStatus()).isEqualTo(TransactionStatus.StatusEnum.CREATED);
    assertThat(transactionEntity.getItemId()).isEqualTo(newCirculationItemId.toString());
    assertThat(transactionEntity.getItemBarcode()).isEqualTo(newBarcode);
    assertThat(transactionEntity.getRequestId()).isEqualTo(newRequestId);
    assertThat(transactionEntity.getLendingLibraryCode()).isEqualTo(newLendingLibraryCode);
    assertThat(transactionEntity.getMaterialType()).isEqualTo(materialType);
  }

    @Test
  void updateTransactionDetailsShouldThrowExceptionWhenNewItemAlreadyExistsInInventory() {
    // TestMate-ced97d9e8821a24f196a9380ce9e8b51
    // Given
    String existingBarcode = "EXISTING_BC";
    TransactionEntity transactionEntity = createTransactionEntity();
    DcbUpdateItem dcbUpdateItem = DcbUpdateItem.builder()
      .barcode(existingBarcode)
      .lendingLibraryCode("LENDER")
      .materialType("book")
      .build();
    DcbItem dcbItem = DcbItem.builder()
      .barcode(existingBarcode)
      .lendingLibraryCode("LENDER")
      .materialType("book")
      .title(transactionEntity.getItemTitle())
      .build();
    ResultList<InventoryItem> existingItems = new ResultList<>();
    existingItems.setTotalRecords(1);
    existingItems.setResult(List.of(new InventoryItem()));
    when(transactionMapper.convertTransactionUpdateItemToDcbItem(dcbUpdateItem, transactionEntity)).thenReturn(dcbItem);
    when(itemService.fetchItemByBarcode(existingBarcode)).thenReturn(existingItems);
    // When
    assertThatThrownBy(() -> baseLibraryService.updateTransactionDetails(transactionEntity, dcbUpdateItem))
      .isInstanceOf(ResourceAlreadyExistException.class)
      .hasMessage("Unable to create item because it already exists in inventory.");
    // Then
    verify(itemService).fetchItemByBarcode(existingBarcode);
    verify(circulationService, never()).cancelRequest(any(), any(Boolean.class));
    verify(transactionRepository, never()).save(any());
  }

    @Test
  void updateTransactionDetailsShouldThrowExceptionWhenNewItemHasOpenTransaction() {
    // TestMate-5935ed35143de432beb5c22b39c79d4e
    // Given
    String barcode = "item-with-open-transaction";
    UUID newItemId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    TransactionEntity transactionEntity = createTransactionEntity();
    transactionEntity.setServicePointId(PICKUP_SERVICE_POINT_ID);
    DcbUpdateItem dcbUpdateItem = DcbUpdateItem.builder()
      .barcode(barcode)
      .lendingLibraryCode("LENDER")
      .materialType("book")
      .build();
    DcbItem dcbItem = DcbItem.builder()
      .barcode(barcode)
      .lendingLibraryCode("LENDER")
      .materialType("book")
      .title(transactionEntity.getItemTitle())
      .build();
    CirculationItem circulationItem = CirculationItem.builder()
      .id(newItemId.toString())
      .barcode(barcode)
      .build();
    when(transactionMapper.convertTransactionUpdateItemToDcbItem(dcbUpdateItem, transactionEntity)).thenReturn(dcbItem);
    when(itemService.fetchItemByBarcode(barcode)).thenReturn(new ResultList<>());
    when(circulationItemService.checkIfItemExistsAndCreate(dcbItem, PICKUP_SERVICE_POINT_ID)).thenReturn(circulationItem);
    when(transactionRepository.findTransactionsByItemIdAndStatusNotInClosed(newItemId))
      .thenReturn(List.of(TransactionEntity.builder().id("other-transaction-id").build()));
    // When
    assertThatThrownBy(() -> baseLibraryService.updateTransactionDetails(transactionEntity, dcbUpdateItem))
      .isInstanceOf(ResourceAlreadyExistException.class)
      .hasMessageContaining(String.format("Item with id %s already has an open DCB transaction", newItemId));
    // Then
    verify(itemService).fetchItemByBarcode(barcode);
    verify(circulationItemService).checkIfItemExistsAndCreate(dcbItem, PICKUP_SERVICE_POINT_ID);
    verify(transactionRepository).findTransactionsByItemIdAndStatusNotInClosed(newItemId);
    verify(circulationService, never()).cancelRequest(any(), any(Boolean.class));
    verify(transactionRepository, never()).save(any());
  }
}
