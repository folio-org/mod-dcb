package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbTransaction;

public interface TransactionAuditService {

    void logTheErrorForExistedTransactionAudit(String dcbTransactionId, String errorMsg);
    void logTheErrorForNotExistedTransactionAudit(String dcbTransactionId, DcbTransaction dcbTransaction, String errorMsg);

}
