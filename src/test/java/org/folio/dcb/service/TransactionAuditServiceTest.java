package org.folio.dcb.service;

import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.repository.TransactionAuditRepository;
import org.folio.dcb.service.impl.TransactionAuditServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.createTransactionAuditEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionAuditServiceTest {

  @InjectMocks
  private TransactionAuditServiceImpl transactionAuditService;
  @Mock
  private TransactionMapper transactionMapper;
  @Mock
  private TransactionAuditRepository transactionAuditRepository;

  @Test
  void logTheErrorForExistedTransactionAuditTest() {
    when(transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(any()))
      .thenReturn(Optional.of(createTransactionAuditEntity()));
    transactionAuditService.logErrorIfTransactionAuditExists(DCB_TRANSACTION_ID, "error_message");
    Mockito.verify(transactionMapper, times(0)).mapToEntity(any(), any());
    Mockito.verify(transactionAuditRepository, times(1)).save(any());
  }
  @Test
  void logTheErrorForNotExistedTransactionAuditTest() {
    when(transactionMapper.mapToEntity(any(), any())).thenReturn(createTransactionEntity());
    when(transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(any()))
      .thenReturn(Optional.empty());
    transactionAuditService.logErrorIfTransactionAuditNotExists(DCB_TRANSACTION_ID, createDcbTransactionByRole(LENDER), "error_message");
    Mockito.verify(transactionMapper, times(1)).mapToEntity(any(), any());
    Mockito.verify(transactionAuditRepository, times(1)).save(any());
  }

}
