package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.repository.TransactionAuditRepository;
import org.folio.dcb.service.TransactionsAuditService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class TransactionsAuditServiceImpl implements TransactionsAuditService {

  private final TransactionAuditRepository transactionAuditRepository;

  @Override
  public void createTransactionAuditRecord(TransactionAuditEntity transactionAuditEntity) {
    log.debug("createTransactionAuditRecord:: creating new transaction audit record with id {} ", transactionAuditEntity.getId());
    transactionAuditRepository.save(transactionAuditEntity);
  }
}
