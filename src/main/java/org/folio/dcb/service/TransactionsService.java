package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;

public interface TransactionsService {
  /**
   * create circulation request
   * @param dcbTransactionId - id of dcb transaction
   * @param dcbTransaction - dcbTransaction entity
   * @return TransactionStatusResponse
   */
  TransactionStatusResponse createCirculationRequest(String dcbTransactionId, DcbTransaction dcbTransaction);
  TransactionStatusResponse updateTransactionStatus(String dcbTransactionId, TransactionStatus transactionStatus);
  TransactionStatusResponse getTransactionStatusById(String dcbTransactionId);
  }
