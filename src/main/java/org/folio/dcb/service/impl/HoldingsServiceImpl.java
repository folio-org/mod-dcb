package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.exception.ResourceNotFoundException;
import org.folio.dcb.client.feign.InventoryHoldingsStorageClient;
import org.folio.dcb.domain.dto.InventoryHolding;
import org.folio.dcb.service.HoldingsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class HoldingsServiceImpl implements HoldingsService {

  private final InventoryHoldingsStorageClient inventoryHoldingsStorageClient;

  @Override
  public InventoryHolding fetchInventoryHoldingDetails(String holdingsId) {
    log.debug("fetchInventoryHoldingDetails:: Trying to fetch holdings detail for holdingsId {}", holdingsId);
    return inventoryHoldingsStorageClient.findHolding(holdingsId)
      .orElseThrow(() -> new ResourceNotFoundException("Holdings not found"));
  }
}
