package org.folio.dcb.service.impl;

import static org.folio.dcb.utils.CqlQuery.exactMatchById;
import static org.folio.dcb.utils.CqlQuery.exactMatchByName;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.InventoryItem;
import org.folio.dcb.domain.dto.MaterialType;
import org.folio.dcb.exception.InventoryItemNotFound;
import org.folio.dcb.integration.invstorage.InventoryItemStorageClient;
import org.folio.dcb.integration.invstorage.MaterialTypeClient;
import org.folio.dcb.service.ItemService;
import org.folio.dcb.utils.CqlQuery;
import org.folio.spring.exception.NotFoundException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Log4j2
public class ItemServiceImpl implements ItemService {

  private final InventoryItemStorageClient inventoryItemStorageClient;
  private final MaterialTypeClient materialTypeClient;

  @Override
  public String fetchItemMaterialTypeIdByMaterialTypeName(String name) {
    log.debug("fetchItemMaterialTypeIdByMaterialTypeName:: Fetching ItemMaterialTypeId by: {}", name);
    return materialTypeClient.fetchMaterialTypeByQuery(exactMatchByName(name).getQuery())
      .getMtypes()
      .stream()
      .findFirst()
      .map(MaterialType::getId)
      .orElseThrow(() -> new NotFoundException(String.format("MaterialType not found with name %s ", name)));
  }

  @Override
  public ResultList<InventoryItem> fetchItemByBarcode(String itemBarcode) {
    log.debug("fetchItemByBarcode:: fetching item details by barcode.");
    return inventoryItemStorageClient.findByQuery(CqlQuery.exactMatch("barcode", itemBarcode).getQuery());
  }

  @Override
  public InventoryItem fetchItemByIdAndBarcode(String id, String barcode) {
    log.debug("fetchItemByBarcode:: fetching item details for id {}.", id);
    var query = CqlQuery.exactMatch("barcode", barcode).and(exactMatchById(id), true).getQuery();
    return inventoryItemStorageClient.findByQuery(query)
      .getResult()
      .stream()
      .findFirst()
      .orElseThrow(() -> new NotFoundException("Unable to find existing item."));
  }

  @Retryable(
    includes = { InventoryItemNotFound.class },
    maxRetriesString = "#{@itemRetryConfiguration.maxRetries}",
    delayString = "#{@itemRetryConfiguration.delayMilliseconds}")
  public InventoryItem findItemByIdAfterCheckIn(String id, String expectedServicePointId) {
    var foundItems = inventoryItemStorageClient.findByQuery(exactMatchById(id).getQuery());
    return Optional.ofNullable(foundItems)
      .map(ResultList::getResult)
      .filter(CollectionUtils::isNotEmpty)
      .map(List::getFirst)
      .filter(item -> hasExpectedServicePointId(expectedServicePointId, item))
      .orElseThrow(() -> new InventoryItemNotFound(String.format(
        "Matched item not found: %s, %s", id, expectedServicePointId)));
  }

  private static boolean hasExpectedServicePointId(String servicePointId, InventoryItem item) {
    return item.getLastCheckIn() != null && Objects.equals(item.getLastCheckIn().getServicePointId(), servicePointId);
  }
}
