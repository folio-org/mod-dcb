package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatusResponse;

public interface LibraryService {
  /**
   * Create transaction
   * @param dcbTransactionId - id of transaction
   * @param dcbTransaction - dcbTransaction Entity
   * @return TransactionStatusResponse
   */
  TransactionStatusResponse createTransaction(String dcbTransactionId, DcbTransaction dcbTransaction);

  /**
   * Update transaction status
   * @param checkInEvent - checkIn event object from kafka
   */
  void updateTransactionStatus(String checkInEvent);
}
