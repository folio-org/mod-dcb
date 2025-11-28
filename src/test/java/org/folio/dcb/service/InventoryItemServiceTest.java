package org.folio.dcb.service;

import org.folio.dcb.client.feign.InventoryItemStorageClient;
import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.ItemLastCheckIn;
import org.folio.dcb.exception.InventoryItemNotFound;
import org.folio.dcb.service.impl.ItemServiceImpl;
import org.folio.spring.exception.NotFoundException;
import org.folio.util.PercentCodec;
import org.folio.util.StringUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.folio.dcb.domain.ResultList.asSinglePage;
import static org.folio.dcb.domain.ResultList.empty;
import static org.folio.dcb.utils.CqlQuery.exactMatchById;
import static org.folio.dcb.utils.EntityUtils.createDcbItem;
import static org.folio.dcb.utils.EntityUtils.createInventoryItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryItemServiceTest {

  @InjectMocks
  private ItemServiceImpl itemService;
  @Mock
  private InventoryItemStorageClient inventoryItemStorageClient;

  @Test
  void fetchItemDetailsByIdAndBarcodeTest() {
    var itemId = UUID.randomUUID().toString();
    var barcode = "DCB_ITEM";
    var inventoryItem = createInventoryItem();
    Appendable queryBarcode = StringUtil.appendCqlEncoded(new StringBuilder("barcode=="), barcode);
    Appendable queryId = StringUtil.appendCqlEncoded(new StringBuilder("id=="), itemId);
    CharSequence query = PercentCodec.encode(queryBarcode + " AND " + queryId);
    when(inventoryItemStorageClient.fetchItemByQuery(query.toString())).thenReturn(ResultList.of(1, List.of(inventoryItem)));

    var response = itemService.fetchItemByIdAndBarcode(itemId, barcode);
    assertEquals(inventoryItem, response);
  }

  @Test
  void fetchItemByIdAndInvalidBarcode() {
    var itemId = UUID.randomUUID().toString();
    var barcode = "DCB_ITEM";
    Appendable queryBarcode = StringUtil.appendCqlEncoded(new StringBuilder("barcode=="), barcode);
    Appendable queryId = StringUtil.appendCqlEncoded(new StringBuilder("id=="), itemId);
    CharSequence query = PercentCodec.encode(queryBarcode + " AND " + queryId);
    when(inventoryItemStorageClient.fetchItemByQuery(query.toString())).thenReturn(ResultList.of(0, List.of()));

    assertThrows(NotFoundException.class, () -> itemService.fetchItemByIdAndBarcode(itemId, barcode));
  }

  @Test
  void fetchItemByBarcode() {
    var item = createDcbItem();
    var inventoryItem = createInventoryItem();
    String query = "barcode==" + StringUtil.cqlEncode(item.getBarcode());
    when(inventoryItemStorageClient.fetchItemByQuery(PercentCodec.encode(query).toString())).thenReturn(ResultList.of(1, List.of(inventoryItem)));

    var response = itemService.fetchItemByBarcode(item.getBarcode());
    assertEquals(1, response.getTotalRecords());
  }

  @Test
  void findItemByIdAfterCheckInWhenItemIsWithValidServicePointId() {
    var expectedServicePointId = UUID.randomUUID().toString();
    var lastCheckIn = new ItemLastCheckIn().servicePointId(expectedServicePointId);
    var item = createInventoryItem().lastCheckIn(lastCheckIn);
    var itemId = item.getId();

    when(inventoryItemStorageClient.fetchItemByQuery(exactMatchById(itemId))).thenReturn(asSinglePage(item));

    var result = itemService.findItemByIdAfterCheckIn(itemId, expectedServicePointId);
    assertEquals(item, result);
  }

  @Test
  void findItemByIdAfterCheckInWhenEmptyResultReturned() {
    var itemId = UUID.randomUUID().toString();
    var servicePointId = UUID.randomUUID().toString();
    when(inventoryItemStorageClient.fetchItemByQuery(exactMatchById(itemId))).thenReturn(empty());

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

    when(inventoryItemStorageClient.fetchItemByQuery(exactMatchById(itemId))).thenReturn(asSinglePage(item));

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

    when(inventoryItemStorageClient.fetchItemByQuery(exactMatchById(itemId))).thenReturn(asSinglePage(item));

    var exception = assertThrows(InventoryItemNotFound.class,
      () -> itemService.findItemByIdAfterCheckIn(itemId, expectedServicePointId));

    var expectedMessage = "Matched item not found: %s, %s".formatted(itemId, expectedServicePointId);
    assertEquals(expectedMessage, exception.getMessage());
  }
}
