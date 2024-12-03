package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.DcbTransactionUpdate;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.dto.TransactionStatusResponseCollection;
import java.time.OffsetDateTime;

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
  TransactionStatusResponseCollection getTransactionStatusList(OffsetDateTime fromDate, OffsetDateTime toDate, Integer pageNumber, Integer pageSize);
  void updateTransactionDetails(String dcbTransactionId, DcbTransactionUpdate dcbTransactionUpdate);

  }
