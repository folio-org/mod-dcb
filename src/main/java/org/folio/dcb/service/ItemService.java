package org.folio.dcb.service;

import org.folio.dcb.domain.dto.InventoryItem;
import org.folio.dcb.exception.InventoryItemNotFound;
import org.folio.spring.model.ResultList;

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

  /**
   * Finds the inventory item by its ID after a check-in operation, ensuring it has the expected service point ID.
   *
   * @param id the unique identifier of the inventory item
   * @param expectedServicePointId the expected service point ID from the last check-in
   * @return the inventory item if found and matching the service point
   * @throws InventoryItemNotFound if the item is not found or does not have the expected service point ID
   */
  InventoryItem findItemByIdAfterCheckIn(String id, String expectedServicePointId);
}
