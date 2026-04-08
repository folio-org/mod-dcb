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
import static org.folio.dcb.utils.EntityUtils.createTransactionAuditEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

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
    when(transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(any()))
      .thenReturn(Optional.empty());
    transactionAuditService.logErrorIfTransactionAuditNotExists(DCB_TRANSACTION_ID, createDcbTransactionByRole(LENDER), "error_message");
    Mockito.verify(transactionAuditRepository, times(1)).save(any());
  }

    @Test
  void testLogErrorIfTransactionAuditNotExistsWhenAuditExistsShouldLogDuplicateError() {
    // TestMate-0f75eabf529985a5592a4c0d84c761b3
    // Given
    String dcbTransactionId = "dup-456";
    String errorMsg = "duplicate detected";
    DcbTransaction dcbTransaction = DcbTransaction.builder()
      .role(DcbTransaction.RoleEnum.BORROWER)
      .build();
    when(transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(dcbTransactionId))
      .thenReturn(Optional.of(new TransactionAuditEntity()));
    ArgumentCaptor<TransactionAuditEntity> auditCaptor = ArgumentCaptor.forClass(TransactionAuditEntity.class);
    // When
    transactionAuditService.logErrorIfTransactionAuditNotExists(dcbTransactionId, dcbTransaction, errorMsg);
    // Then
    verify(transactionAuditRepository).save(auditCaptor.capture());
    TransactionAuditEntity savedAudit = auditCaptor.getValue();
    assertThat(savedAudit.getAction()).isEqualTo("DUPLICATE_ERROR");
    assertThat(savedAudit.getTransactionId()).isEqualTo("-1");
    assertThat(savedAudit.getErrorMessage())
      .contains(dcbTransactionId)
      .contains("BORROWER")
      .contains(errorMsg);
  }

    @Test
  void testLogErrorIfTransactionAuditNotExistsWhenDcbTransactionIsNull() {
    // TestMate-c6ffce49ce9c7a9bf08e4766a30a7e62
    // Given
    String dcbTransactionId = "id-999";
    String errorMsg = "null object error";
    DcbTransaction dcbTransaction = null;
    when(transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(dcbTransactionId))
      .thenReturn(Optional.empty());
    ArgumentCaptor<TransactionAuditEntity> auditCaptor = ArgumentCaptor.forClass(TransactionAuditEntity.class);
    // When
    transactionAuditService.logErrorIfTransactionAuditNotExists(dcbTransactionId, dcbTransaction, errorMsg);
    // Then
    verify(transactionAuditRepository).save(auditCaptor.capture());
    TransactionAuditEntity savedAudit = auditCaptor.getValue();
    assertThat(savedAudit.getTransactionId()).isEqualTo(dcbTransactionId);
    assertThat(savedAudit.getAction()).isEqualTo("ERROR");
    assertThat(savedAudit.getBefore()).isNull();
    assertThat(savedAudit.getAfter()).isNull();
    assertThat(savedAudit.getErrorMessage())
      .contains("dcbTransactionId = id-999")
      .contains("role = null")
      .contains("error message = null object error");
  }

}
