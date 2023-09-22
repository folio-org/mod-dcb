package org.folio.dcb.service;

import org.folio.dcb.domain.dto.InventoryItem;

public interface ItemService {
  InventoryItem fetchItemDetailsById(String itemId);
}
