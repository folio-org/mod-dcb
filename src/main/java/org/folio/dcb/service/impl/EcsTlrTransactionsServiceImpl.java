package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.service.EcsTlrTransactionsService;
import org.springframework.stereotype.Service;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;

@Service
@RequiredArgsConstructor
@Log4j2
public class EcsTlrTransactionsServiceImpl implements EcsTlrTransactionsService {

  private final BaseTransactionsService baseTransactionsService;
  private final BaseLibraryService baseLibraryService;

  @Override
  public TransactionStatusResponse createEcsTlrTransaction(String dcbTransactionId, DcbTransaction dcbTransaction) {
    log.debug("createCirculationRequest:: creating new transaction request for role {} ", dcbTransaction.getRole());
    baseTransactionsService.checkTransactionExistsAndThrow(dcbTransactionId);
    if (dcbTransaction.getRole() == LENDER) {
       baseLibraryService.saveDcbTransaction(dcbTransactionId, dcbTransaction, dcbTransaction.getRequestId());
    } else if (dcbTransaction.getRole() == BORROWER) {
       baseLibraryService.saveDcbTransaction(dcbTransactionId, dcbTransaction, dcbTransaction.getRequestId());
    } else {
      throw new IllegalArgumentException("Unimplemented role: " + dcbTransaction.getRole());
    }
    return TransactionStatusResponse.builder()
      .status(TransactionStatusResponse.StatusEnum.CREATED)
      .item(dcbTransaction.getItem())
      .patron(dcbTransaction.getPatron())
      .build();
  }
}
