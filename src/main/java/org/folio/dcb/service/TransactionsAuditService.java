package org.folio.dcb.service;

import org.folio.dcb.domain.entity.TransactionAuditEntity;

public interface TransactionsAuditService {

  void createTransactionAuditRecord(TransactionAuditEntity transactionAuditEntity);
}
