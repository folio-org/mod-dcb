package org.folio.dcb.service;

import org.folio.dcb.domain.dto.InventoryHolding;

public interface HoldingsService {
  InventoryHolding fetchInventoryHoldingDetails(String holdingsId);
}
