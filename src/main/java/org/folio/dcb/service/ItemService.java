package org.folio.dcb.service;

import org.folio.dcb.domain.dto.InventoryItem;

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
  String fetchItemMaterialTypeNameByMaterialTypeId(String materialTypeId);
}
