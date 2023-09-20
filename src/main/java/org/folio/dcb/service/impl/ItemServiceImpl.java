package org.folio.dcb.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.exception.ResourceNotFoundException;
import org.folio.dcb.client.feign.InventoryItemStorageClient;
import org.folio.dcb.domain.dto.InventoryItem;
import org.folio.dcb.service.ItemService;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Log4j2
public class ItemServiceImpl implements ItemService {

  private final InventoryItemStorageClient inventoryItemStorageClient;

  @Override
  public InventoryItem fetchItemDetailsById(String itemId) {
    log.debug("fetchItemDetailsById:: Trying to fetch item details for itemId {}", itemId);
    return inventoryItemStorageClient.findItem(itemId)
      .orElseThrow(() -> new ResourceNotFoundException("Item not found for itemId " + itemId));
  }

}
