package org.folio.dcb.service;

import org.folio.dcb.client.feign.HoldingsStorageClient;

public interface HoldingsService {
  /**
   * Get holding details of an inventory by holdingId
   * @param holdingId - id of holding
   * @return InventoryHolding
   */
  HoldingsStorageClient.Holding fetchInventoryHoldingDetailsByHoldingId(String holdingId);
}
