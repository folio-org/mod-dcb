package org.folio.dcb.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.InventoryItemStorageClient;
import org.folio.dcb.client.feign.MaterialTypeClient;
import org.folio.dcb.domain.dto.InventoryItem;
import org.folio.dcb.service.ItemService;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.model.ResultList;
import org.folio.util.PercentCodec;
import org.folio.util.StringUtil;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Log4j2
public class ItemServiceImpl implements ItemService {

  private final InventoryItemStorageClient inventoryItemStorageClient;
  private final MaterialTypeClient materialTypeClient;

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
    return inventoryItemStorageClient.fetchItemByQuery("barcode==" + StringUtil.urlEncode(itemBarcode));
  }

  @Override
  public InventoryItem fetchItemByIdAndBarcode(String id, String barcode) {
    log.debug("fetchItemByBarcode:: fetching item details for id {} , barcode {} ", id, barcode);
    Appendable queryBarcode = StringUtil.appendCqlEncoded(new StringBuilder("barcode=="), barcode);
    Appendable queryId = StringUtil.appendCqlEncoded(new StringBuilder("id=="), id);
    CharSequence query = PercentCodec.encode(queryBarcode + " AND " + queryId);
    return inventoryItemStorageClient.fetchItemByQuery(query.toString())
      .getResult()
      .stream()
      .findFirst()
      .orElseThrow(() -> new NotFoundException(String.format("Unable to find existing item with id %s and barcode %s.", id, barcode)));
  }

}
