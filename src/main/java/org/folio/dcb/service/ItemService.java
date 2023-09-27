package org.folio.dcb.service;

import org.folio.dcb.domain.dto.InventoryItem;

public interface ItemService {
  /**
   * Get item details of an inventory by itemId
   * @param itemId - id of an item
   * @return InventoryItem
   */
  InventoryItem fetchItemDetailsById(String itemId);
}
