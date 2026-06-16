package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatusResponse;

public interface EcsRequestTransactionsService {

  TransactionStatusResponse createEcsRequestTransactions(String transactionId, DcbTransaction dcbTransaction);

  TransactionStatusResponse updateEcsRequestTransaction(String transactionId, DcbTransaction dcbTransaction);
}
