package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;

public interface LibraryService {
  /**
   * Create transaction
   * @param dcbTransactionId - id of transaction
   * @param dcbTransaction - dcbTransaction Entity
   * @return TransactionStatusResponse
   */
  TransactionStatusResponse createCirculation(String dcbTransactionId, DcbTransaction dcbTransaction, String pickupServicePointId);

  void updateTransactionStatus(TransactionEntity dcbTransaction, TransactionStatus transactionStatus);
}
