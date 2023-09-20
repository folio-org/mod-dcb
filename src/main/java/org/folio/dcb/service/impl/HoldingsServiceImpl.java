package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.InventoryHoldingsStorageClient;
import org.folio.dcb.domain.dto.InventoryHolding;
import org.folio.dcb.service.HoldingsService;
import org.folio.spring.exception.NotFoundException;
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
      .orElseThrow(() -> new NotFoundException(String.format("Holdings not found for holdings id %s", holdingsId)));
  }
}
