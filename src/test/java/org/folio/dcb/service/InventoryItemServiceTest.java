package org.folio.dcb.service;

import org.folio.dcb.client.feign.InventoryItemStorageClient;
import org.folio.dcb.service.impl.ItemServiceImpl;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.folio.dcb.utils.EntityUtils.createInventoryItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryItemServiceTest {

  @InjectMocks
  private ItemServiceImpl itemService;
  @Mock
  private InventoryItemStorageClient inventoryItemStorageClient;

  @Test
  void fetchItemDetailsByIdTest() {
    var itemId = UUID.randomUUID().toString();
    var inventoryItem = createInventoryItem();
    when(inventoryItemStorageClient.findItem(itemId)).thenReturn(Optional.of(inventoryItem));
    var response = itemService.fetchItemDetailsById(itemId);
    verify(inventoryItemStorageClient).findItem(itemId);
    assertEquals(inventoryItem, response);
  }

  @Test
  void fetchItemDetailsByInvalidIdTest() {
    var itemId = UUID.randomUUID().toString();
    when(inventoryItemStorageClient.findItem(itemId)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> itemService.fetchItemDetailsById(itemId));
  }

}
