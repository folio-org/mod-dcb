package org.folio.dcb.service.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.InventoryItemStorageClient;
import org.folio.dcb.client.feign.MaterialTypeClient;
import org.folio.dcb.domain.dto.InventoryItem;
import org.folio.dcb.service.ItemService;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.model.ResultList;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Log4j2
public class ItemServiceImpl implements ItemService {

  private final InventoryItemStorageClient inventoryItemStorageClient;
  private final MaterialTypeClient materialTypeClient;

  @Override
  public InventoryItem fetchItemDetailsById(String itemId) {
    log.debug("fetchItemDetailsById:: Trying to fetch item details for itemId {}", itemId);
    try {
      return inventoryItemStorageClient.findItem(itemId);
    } catch (FeignException.NotFound ex) {
      throw new NotFoundException(String.format("Item not found for itemId %s ", itemId));
    }
  }

  @Override
  public String fetchItemMaterialTypeIdByMaterialTypeName(String materialTypeName) {
    log.debug("fetchItemMaterialTypeIdByMaterialTypeName:: Fetching ItemMaterialTypeId by MaterialTypeName={}", materialTypeName);
    return materialTypeClient.fetchMaterialTypeByQuery(String.format("name==\"%s\"", materialTypeName))
      .getMtypes()
      .stream()
      .findFirst()
      .map(org.folio.dcb.domain.dto.MaterialType::getId)
      .orElseThrow(() -> new NotFoundException(String.format("MaterialType not found with name %s ", materialTypeName)));
  }

  @Override
  public ResultList<InventoryItem> fetchItemByBarcode(String itemBarcode) {
    log.debug("fetchItemByBarcode:: fetching item details for barcode {} ", itemBarcode);
    return inventoryItemStorageClient.fetchItemByBarcode("barcode==" + itemBarcode);
  }

}
