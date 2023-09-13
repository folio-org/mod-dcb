package org.folio.dcb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.entity.Transactions;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.repository.TransactionsRepository;
import org.folio.spring.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class TransactionService {

  private final TransactionsRepository transactionsRepository;

  public TransactionStatus getTransactionStatusById(UUID transactionId) {
    Transactions transaction = getTransactionOrThrow(transactionId);
    TransactionStatus ts = new TransactionStatus();
    ts.setStatus(transaction.getStatus());
    ts.setMessage("message");
    return ts;
  }

  private Transactions getTransactionOrThrow(UUID transactionId) {
    return transactionsRepository.findById(transactionId)
      .orElseThrow(() -> new NotFoundException("DCB Transaction was not found by id=" + transactionId));
  }

}
