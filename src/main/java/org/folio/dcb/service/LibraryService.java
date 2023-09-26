package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;

public interface LibraryService {
  TransactionStatusResponse createTransaction(String dcbTransactionId, DcbTransaction dcbTransaction);
  void updateTransactionStatus(TransactionEntity dcbTransaction, TransactionStatus transactionStatus);
}
