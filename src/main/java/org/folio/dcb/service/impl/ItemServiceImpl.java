package org.folio.dcb.service.impl;

import static org.folio.dcb.utils.CqlQuery.exactMatchById;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.dcb.client.feign.InventoryItemStorageClient;
import org.folio.dcb.client.feign.MaterialTypeClient;
import org.folio.dcb.domain.dto.InventoryItem;
import org.folio.dcb.exception.InventoryItemNotFound;
import org.folio.dcb.service.ItemService;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.model.ResultList;
import org.folio.util.PercentCodec;
import org.folio.util.StringUtil;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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
    String query = "barcode==" + StringUtil.cqlEncode(itemBarcode);
    return inventoryItemStorageClient.fetchItemByQuery(PercentCodec.encode(query).toString());
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

  @Retryable(
    retryFor = InventoryItemNotFound.class,
    maxAttemptsExpression = "#{@itemRetryConfiguration.maxRetries}",
    backoff = @Backoff(delayExpression = "#{@itemRetryConfiguration.delayMilliseconds}"))
  public InventoryItem findItemByIdAfterCheckIn(String id, String expectedServicePointId) {
    var foundItems = inventoryItemStorageClient.fetchItemByQuery(exactMatchById(id));
    return Optional.ofNullable(foundItems)
      .map(ResultList::getResult)
      .filter(CollectionUtils::isNotEmpty)
      .map(List::getFirst)
      .filter(item -> hasExpectedServicePointId(expectedServicePointId, item))
      .orElseThrow(() -> new InventoryItemNotFound(String.format(
        "Matched item not found: %s, %s", id, expectedServicePointId)));
  }

  private static boolean hasExpectedServicePointId(String servicePointId, InventoryItem item) {
    return item.getLastCheckIn() != null
      && Objects.equals(item.getLastCheckIn().getServicePointId(), servicePointId);
  }
}
