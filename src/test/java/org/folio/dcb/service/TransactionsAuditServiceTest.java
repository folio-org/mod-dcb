package org.folio.dcb.service;

import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.repository.TransactionAuditRepository;
import org.folio.dcb.service.impl.TransactionsAuditServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionsAuditServiceTest {

  @InjectMocks
  private TransactionsAuditServiceImpl transactionsAuditService;
  @Mock
  private TransactionAuditRepository transactionAuditRepository;

  @Test
  void createTransactionAuditRecordTest() {
    TransactionAuditEntity transactionAuditEntity = TransactionAuditEntity.builder().transactionId("123").build();
    transactionsAuditService.createTransactionAuditRecord(transactionAuditEntity);
    verify(transactionAuditRepository).save(any());
  }
}
