package org.folio.dcb.service;

import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.InventoryItem;

public interface ItemService {

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
  /**
   * Get item details of an inventory by item id and Barcode
   * @param itemBarcode - barcode of an item
   * @param id - id of an item
   * @return InventoryItem
   */
  InventoryItem fetchItemByIdAndBarcode(String id, String itemBarcode);
}
