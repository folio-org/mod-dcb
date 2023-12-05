package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbTransaction;

public interface TransactionAuditService {

    void logErrorIfTransactionAuditExists(String dcbTransactionId, String errorMsg);
    void logErrorIfTransactionAuditNotExists(String dcbTransactionId, DcbTransaction dcbTransaction, String errorMsg);

}
