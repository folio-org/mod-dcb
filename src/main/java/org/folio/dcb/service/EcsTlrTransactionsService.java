package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatusResponse;

public interface EcsTlrTransactionsService {
  TransactionStatusResponse createEcsTlrTransaction(String dcbTransactionId, DcbTransaction dcbTransaction);
}
