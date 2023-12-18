package org.folio.dcb.service;

import org.folio.dcb.client.feign.InventoryItemStorageClient;
import org.folio.dcb.service.impl.ItemServiceImpl;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

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
    when(inventoryItemStorageClient.fetchItemByQuery("barcode==" + barcode + " and id==" + itemId)).thenReturn(ResultList.of(1, List.of(inventoryItem)));

    var response = itemService.fetchItemByIdAndBarcode(itemId, barcode);
    assertEquals(inventoryItem, response);
  }

  @Test
  void fetchItemByIdAndInvalidBarcode() {
    var itemId = UUID.randomUUID().toString();
    var barcode = "DCB_ITEM";
    when(inventoryItemStorageClient.fetchItemByQuery("barcode==" + barcode + " and id==" + itemId)).thenReturn(ResultList.of(0, List.of()));

    assertThrows(NotFoundException.class, () -> itemService.fetchItemByIdAndBarcode(itemId, barcode));
  }

  @Test
  void fetchItemByBarcode() {
    var item = createDcbItem();
    var inventoryItem = createInventoryItem();
    when(inventoryItemStorageClient.fetchItemByQuery("barcode==" + item.getBarcode())).thenReturn(ResultList.of(1, List.of(inventoryItem)));

    var response = itemService.fetchItemByBarcode(item.getBarcode());
    assertEquals(1, response.getTotalRecords());
  }

}
