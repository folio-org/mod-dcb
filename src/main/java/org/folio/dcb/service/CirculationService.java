package org.folio.dcb.service;

import org.folio.dcb.domain.entity.TransactionEntity;

public interface CirculationService {
  /**
   * Check in item by barcode
   * @param dcbTransaction dcbTransactionEntity
   * @return
   */
  void checkInByBarcode(TransactionEntity dcbTransaction);

  void checkInByBarcode(TransactionEntity dcbTransaction, String servicePointId);

  /**
   * Check out item by barcode
   * @param dcbTransaction dcbTransactionEntity
   * @return
   */
  void checkOutByBarcode(TransactionEntity dcbTransaction);

}
