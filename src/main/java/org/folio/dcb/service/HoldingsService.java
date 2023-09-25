package org.folio.dcb.service;

import org.folio.dcb.domain.dto.InventoryHolding;

public interface HoldingsService {
  /**
   * Get holding details of an inventory by holdingId
   * @param holdingId - id of holding
   * @return InventoryHolding
   */
  InventoryHolding fetchInventoryHoldingDetailsByHoldingId(String holdingId);
}
