package org.folio.dcb.service.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.InventoryItemStorageClient;
import org.folio.dcb.client.feign.LoanTypeClient;
import org.folio.dcb.client.feign.MaterialTypeClient;
import org.folio.dcb.domain.dto.InventoryItem;
import org.folio.dcb.service.ItemService;
import org.folio.spring.exception.NotFoundException;
import org.springframework.stereotype.Service;

import static org.folio.dcb.utils.DCBConstants.DCB_LOAN_TYPE_NAME;

@Service
@AllArgsConstructor
@Log4j2
public class ItemServiceImpl implements ItemService {

  private final InventoryItemStorageClient inventoryItemStorageClient;
  private final MaterialTypeClient materialTypeClient;
  private final LoanTypeClient loanTypeClient;

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
  public String fetchItemMaterialTypeNameByMaterialTypeId(String materialTypeId) {
    log.debug("fetchItemMaterialTypeNameByMaterialTypeId:: Fetching ItemMaterialTypeName by MaterialTypeId={}", materialTypeId);
    return materialTypeClient.fetchMaterialTypeByQuery(String.format("id==\"%s\"", materialTypeId))
      .getMtypes()
      .stream()
      .findFirst()
      .map(org.folio.dcb.domain.dto.MaterialType::getName)
      .orElseThrow(() -> new NotFoundException(String.format("MaterialType not found with id %s ", materialTypeId)));
  }

  @Override
  public String fetchItemLoanTypeIdByLoanTypeName(String loanTypeName) {
    log.debug("fetchItemLoanTypeIdByLoanTypeName:: Fetching ItemMaterialTypeId by MaterialTypeName={}", loanTypeName);
    return loanTypeClient.queryLoanTypeByName(DCB_LOAN_TYPE_NAME)
      .getResult()
      .stream()
      .findFirst()
      .map(LoanTypeClient.LoanType::getId)
      .orElseThrow(() -> new NotFoundException(String.format("LoanType not found with name %s ", loanTypeName)));  }
}
