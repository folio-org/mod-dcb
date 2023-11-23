package org.folio.dcb.service;

import org.folio.dcb.domain.dto.InventoryItem;
import org.folio.spring.model.ResultList;

public interface ItemService {
  /**
   * Get item details of an inventory by itemId
   * @param itemId - id of an item
   * @return InventoryItem
   */
  InventoryItem fetchItemDetailsById(String itemId);

  /**
   * Provides material type Id by material type name.
   * @param materialTypeName  - material type name like 'book', 'sound recording', 'text' etc.
   * @return String value of material type Id.
   * */
  String fetchItemMaterialTypeIdByMaterialTypeName(String materialTypeName);

  /**
   * Get item details of an inventory by itemBarcode
   * @param itemBarcode - barcode of an item
   * @return InventoryItem
   */
  ResultList<InventoryItem> fetchItemByBarcode(String itemBarcode);
}
