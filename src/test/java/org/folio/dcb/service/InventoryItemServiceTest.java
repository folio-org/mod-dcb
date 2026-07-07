package org.folio.dcb.service;

import static org.folio.dcb.domain.ResultList.asSinglePage;
import static org.folio.dcb.domain.ResultList.empty;
import static org.folio.dcb.utils.CqlQuery.exactMatchById;
import static org.folio.dcb.utils.EntityUtils.createDcbItem;
import static org.folio.dcb.utils.EntityUtils.createInventoryItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.ItemLastCheckIn;
import org.folio.dcb.domain.dto.MaterialType;
import org.folio.dcb.domain.dto.MaterialTypeCollection;
import org.folio.dcb.exception.InventoryItemNotFound;
import org.folio.dcb.integration.invstorage.InventoryItemStorageClient;
import org.folio.dcb.integration.invstorage.MaterialTypeClient;
import org.folio.dcb.service.impl.ItemServiceImpl;
import org.folio.dcb.utils.CqlQuery;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryItemServiceTest {

  @InjectMocks private ItemServiceImpl itemService;
  @Mock private MaterialTypeClient materialTypeClient;
  @Mock private InventoryItemStorageClient inventoryItemStorageClient;

  @Test
  void fetchItemDetailsByIdAndBarcodeTest() {
    var itemId = UUID.randomUUID().toString();
    var barcode = "DCB_ITEM";
    var inventoryItem = createInventoryItem();
    var query = CqlQuery.exactMatch("barcode", barcode).and(exactMatchById(itemId), true).getQuery();
    when(inventoryItemStorageClient.findByQuery(query)).thenReturn(ResultList.of(1, List.of(inventoryItem)));

    var response = itemService.fetchItemByIdAndBarcode(itemId, barcode);
    assertEquals(inventoryItem, response);
  }

  @Test
  void fetchItemByIdAndInvalidBarcode() {
    var itemId = UUID.randomUUID().toString();
    var barcode = "DCB_ITEM";
    var query = CqlQuery.exactMatch("barcode", barcode).and(exactMatchById(itemId), true).getQuery();
    when(inventoryItemStorageClient.findByQuery(query)).thenReturn(ResultList.empty());

    assertThrows(NotFoundException.class, () -> itemService.fetchItemByIdAndBarcode(itemId, barcode));
  }

  @Test
  void fetchItemByBarcode() {
    var item = createDcbItem();
    var inventoryItem = createInventoryItem();
    var query = CqlQuery.exactMatch("barcode", item.getBarcode()).getQuery();
    when(inventoryItemStorageClient.findByQuery(query)).thenReturn(ResultList.of(1, List.of(inventoryItem)));

    var response = itemService.fetchItemByBarcode(item.getBarcode());
    assertEquals(1, response.getTotalRecords());
  }

  @Test
  void findItemByIdAfterCheckInWhenItemIsWithValidServicePointId() {
    var expectedServicePointId = UUID.randomUUID().toString();
    var lastCheckIn = new ItemLastCheckIn().servicePointId(expectedServicePointId);
    var item = createInventoryItem().lastCheckIn(lastCheckIn);
    var itemId = item.getId();

    var query = exactMatchById(itemId).getQuery();
    when(inventoryItemStorageClient.findByQuery(query)).thenReturn(asSinglePage(item));

    var result = itemService.findItemByIdAfterCheckIn(itemId, expectedServicePointId);
    assertEquals(item, result);
  }

  @Test
  void findItemByIdAfterCheckInWhenEmptyResultReturned() {
    var itemId = UUID.randomUUID().toString();
    var servicePointId = UUID.randomUUID().toString();
    var expectedQuery = exactMatchById(itemId).getQuery();
    when(inventoryItemStorageClient.findByQuery(expectedQuery)).thenReturn(empty());

    var exception = assertThrows(InventoryItemNotFound.class,
      () -> itemService.findItemByIdAfterCheckIn(itemId, servicePointId));

    var expectedMessage = "Matched item not found: %s, %s".formatted(itemId, servicePointId);
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void findItemByIdAfterCheckInWhenItemIsWithoutLastCheckIn() {
    var servicePointId = UUID.randomUUID().toString();
    var item = createInventoryItem();
    var itemId = item.getId();

    var query = exactMatchById(itemId).getQuery();
    when(inventoryItemStorageClient.findByQuery(query)).thenReturn(asSinglePage(item));

    var exception = assertThrows(InventoryItemNotFound.class,
      () -> itemService.findItemByIdAfterCheckIn(itemId, servicePointId));

    var expectedMessage = "Matched item not found: %s, %s".formatted(itemId, servicePointId);
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void findItemByIdAfterCheckInWhenItemIsWithInvalidServicePointId() {
    var expectedServicePointId = UUID.randomUUID().toString();
    var lastCheckIn = new ItemLastCheckIn().servicePointId(UUID.randomUUID().toString());
    var item = createInventoryItem().lastCheckIn(lastCheckIn);
    var itemId = item.getId();

    var query = exactMatchById(itemId).getQuery();
    when(inventoryItemStorageClient.findByQuery(query)).thenReturn(asSinglePage(item));

    var exception = assertThrows(InventoryItemNotFound.class,
      () -> itemService.findItemByIdAfterCheckIn(itemId, expectedServicePointId));

    var expectedMessage = "Matched item not found: %s, %s".formatted(itemId, expectedServicePointId);
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void fetchItemMaterialTypeIdByMaterialTypeNameTest() {
    // TestMate-0f96f8c42d03650b357196259cdf7e6b
    var materialTypeName = "book";
    var materialTypeId = "1a2b3c4d-5e6f-7g8h-9i0j";
    var materialType = new MaterialType().id(materialTypeId).name(materialTypeName);
    var materialTypeResultList = new MaterialTypeCollection().addMtypesItem(materialType).totalRecords(1);
    when(materialTypeClient.fetchMaterialTypeByQuery(anyString())).thenReturn(materialTypeResultList);

    var result = itemService.fetchItemMaterialTypeIdByMaterialTypeName(materialTypeName);

    verify(materialTypeClient).fetchMaterialTypeByQuery("name==\"book\"");
    assertEquals(materialTypeId, result);
  }

  @Test
  void fetchItemMaterialTypeIdByInvalidNameTest() {
    // TestMate-772865b75acf5b12a75b8a6a3937b4b1
    var name = "non-existent-type";
    var materialTypes = new MaterialTypeCollection().totalRecords(0);
    when(materialTypeClient.fetchMaterialTypeByQuery(anyString())).thenReturn(materialTypes);
    assertThrows(NotFoundException.class, () -> itemService.fetchItemMaterialTypeIdByMaterialTypeName(name));
    verify(materialTypeClient).fetchMaterialTypeByQuery("name==\"non-existent-type\"");
  }

  @Test
  void fetchItemMaterialTypeIdByMultipleResultsTest() {
    // TestMate-4b6d139cbb3dac517781e88fd0bd5828
    var materialTypeName = "text";
    var mtypeId1 = "11111111-1111-1111-1111-111111111111";
    var mtypeId2 = "22222222-2222-2222-2222-222222222222";
    var firstMaterialType = new MaterialType().id(mtypeId1).name(materialTypeName);
    var secondMaterialType = new MaterialType().id(mtypeId2).name(materialTypeName);
    var materialTypeResultList = new MaterialTypeCollection()
      .addMtypesItem(firstMaterialType)
      .addMtypesItem(secondMaterialType)
      .totalRecords(2);

    when(materialTypeClient.fetchMaterialTypeByQuery(anyString())).thenReturn(materialTypeResultList);

    var result = itemService.fetchItemMaterialTypeIdByMaterialTypeName(materialTypeName);

    verify(materialTypeClient).fetchMaterialTypeByQuery("name==\"text\"");
    assertEquals(mtypeId1, result);
  }
}
