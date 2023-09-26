package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.TransactionsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Log4j2
public class TransactionsServiceImpl implements TransactionsService {

  @Qualifier("lendingLibraryService")
  private final LibraryService lendingLibraryService;

  private final TransactionRepository transactionRepository;

  @Override
  public TransactionStatusResponse createCirculationRequest(String dcbTransactionId, DcbTransaction dcbTransaction) {
    log.debug("createCirculationRequest:: creating new transaction request for role {} ", dcbTransaction.getRole());
    return switch (dcbTransaction.getRole()) {
      case LENDER -> lendingLibraryService.createTransaction(dcbTransactionId, dcbTransaction);
      default -> throw new IllegalArgumentException("Other roles are not implemented");
    };
  }

  @Override
  public TransactionStatusResponse updateTransactionStatus(String dcbTransactionId, TransactionStatus transactionStatus) {
    return transactionRepository.findById(dcbTransactionId).map(dcbTransaction -> {
      if (Objects.requireNonNull(dcbTransaction.getRole()) == DcbTransaction.RoleEnum.LENDER) {
        lendingLibraryService.updateTransactionStatus(dcbTransaction, transactionStatus);
      } else {
        throw new IllegalArgumentException("Other roles are not implemented");
      }
      return TransactionStatusResponse.builder()
        .message("Status updated")
        .status(TransactionStatusResponse.StatusEnum.fromValue(transactionStatus.getStatus().getValue()))
        .build();
    }).orElseThrow(() -> new IllegalArgumentException(String.format("Transaction with id %s not found", dcbTransactionId)));
  }
}
