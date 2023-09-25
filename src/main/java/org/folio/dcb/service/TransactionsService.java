package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatusResponse;

public interface TransactionsService {
  String DCB_TRANSACTION_NOT_FOUND_BU_ID = "DCB Transaction was not found by id=";
  TransactionStatusResponse createCirculationRequest(String dcbTransactionId, DcbTransaction dcbTransaction);
  TransactionStatusResponse getTransactionStatusById(String dcbTransactionId);

  }
