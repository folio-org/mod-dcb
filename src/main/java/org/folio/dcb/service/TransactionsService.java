package org.folio.dcb.service;

import java.time.OffsetDateTime;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.DcbUpdateTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.dto.TransactionStatusResponseCollection;

public interface TransactionsService {
  /**
   * Create a circulation request.
   *
   * @param dcbTransactionId - id of dcb transaction
   * @param dcbTransaction - dcbTransaction entity
   * @return TransactionStatusResponse
   */
  TransactionStatusResponse createCirculationRequest(String dcbTransactionId, DcbTransaction dcbTransaction);

  TransactionStatusResponse updateTransactionStatus(String dcbTransactionId, TransactionStatus transactionStatus);

  TransactionStatusResponse getTransactionStatusById(String dcbTransactionId);

  TransactionStatusResponseCollection getTransactionStatusList(OffsetDateTime fromDate, OffsetDateTime toDate,
      Integer pageNumber, Integer pageSize);

  void updateTransactionDetails(String dcbTransactionId, DcbUpdateTransaction dcbUpdateTransaction);

  TransactionStatusResponse renewLoanByTransactionId(String dcbTransactionId);

  /**
   * Blocks item renewal for a DCB transaction.
   * This prevents the item from being renewed until unblocked.
   *
   * @param dcbTransactionId unique identifier of the DCB transaction
   */
  void blockItemRenewalByTransactionId(String dcbTransactionId);

  /**
   * Unblocks item renewal for a DCB transaction.
   * This allows the item to be renewed again after being blocked.
   *
   * @param dcbTransactionId unique identifier of the DCB transaction
   */
  void unblockItemRenewalByTransactionId(String dcbTransactionId);
}
