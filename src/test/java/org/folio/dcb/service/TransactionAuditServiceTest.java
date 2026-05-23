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
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWER;
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
    // TestMate-fa5231f9fd8fb40f55082244d97512b7
    // Given
    String errorMsg = "Duplicate ID";
    var dcbTransaction = createDcbTransactionByRole(BORROWER);
    TransactionAuditEntity existingAudit = createTransactionAuditEntity();
    when(transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(DCB_TRANSACTION_ID))
      .thenReturn(Optional.of(existingAudit));
    // When
    transactionAuditService.logErrorIfTransactionAuditNotExists(DCB_TRANSACTION_ID, dcbTransaction, errorMsg);
    // Then
    ArgumentCaptor<TransactionAuditEntity> captor = ArgumentCaptor.forClass(TransactionAuditEntity.class);
    verify(transactionAuditRepository).save(captor.capture());
    TransactionAuditEntity savedAudit = captor.getValue();
    assertThat(savedAudit.getAction()).isEqualTo("DUPLICATE_ERROR");
    assertThat(savedAudit.getTransactionId()).isEqualTo("-1");
    assertThat(savedAudit.getBefore()).isNull();
    assertThat(savedAudit.getAfter()).isNull();
    assertThat(savedAudit.getErrorMessage())
      .isEqualTo(String.format("dcbTransactionId = %s; role = %s; error message = %s.", DCB_TRANSACTION_ID, BORROWER, errorMsg));
  }

    @Test
  void testLogErrorIfTransactionAuditNotExistsWhenDcbTransactionIsNull() {
    // TestMate-dbb7ede2a4d44cf4e63907539abcc619
    // Given
    String dcbTransactionId = "trn-456";
    String errorMsg = "Null payload";
    when(transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(dcbTransactionId))
      .thenReturn(Optional.empty());
    // When
    transactionAuditService.logErrorIfTransactionAuditNotExists(dcbTransactionId, null, errorMsg);
    // Then
    ArgumentCaptor<TransactionAuditEntity> captor = ArgumentCaptor.forClass(TransactionAuditEntity.class);
    verify(transactionAuditRepository).save(captor.capture());
    TransactionAuditEntity savedAudit = captor.getValue();
    assertThat(savedAudit.getAction()).isEqualTo("ERROR");
    assertThat(savedAudit.getTransactionId()).isEqualTo(dcbTransactionId);
    assertThat(savedAudit.getBefore()).isNull();
    assertThat(savedAudit.getAfter()).isNull();
    assertThat(savedAudit.getErrorMessage())
      .isEqualTo("dcbTransactionId = trn-456; role = null; error message = Null payload.");
  }

}
