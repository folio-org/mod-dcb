package org.folio.dcb.service;

import org.folio.dcb.client.feign.HoldingsStorageClient;

public interface HoldingsService {
  /**
   * Get holding details of an inventory by holdingId
   * @param holdingId - id of holding
   * @return InventoryHolding
   */
  HoldingsStorageClient.Holding fetchInventoryHoldingDetailsByHoldingId(String holdingId);

  /**
   * Get predefined DCB holding or create a new one if it does not exist.
   * @return DCB Holding
   */
  HoldingsStorageClient.Holding fetchDcbHoldingOrCreateIfMissing();
}
