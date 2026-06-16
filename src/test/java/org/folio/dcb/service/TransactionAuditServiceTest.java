package org.folio.dcb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.borrowerDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createTransactionAuditEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.repository.TransactionAuditRepository;
import org.folio.dcb.service.impl.TransactionAuditServiceImpl;
import org.folio.dcb.support.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TransactionAuditServiceTest {

  @InjectMocks private TransactionAuditServiceImpl transactionAuditService;
  @Mock private TransactionMapper transactionMapper;
  @Mock private TransactionAuditRepository repository;

  @Test
  void logTheErrorForExistedTransactionAuditTest() {
    when(repository.findLatestTransactionAuditEntityByDcbTransactionId(any()))
      .thenReturn(Optional.of(createTransactionAuditEntity()));
    transactionAuditService.logErrorIfTransactionAuditExists(DCB_TRANSACTION_ID, "error_message");
    verify(transactionMapper, times(0)).mapToEntity(any(), any());
    verify(repository).save(any());
  }

  @Test
  void logTheErrorForNotExistedTransactionAuditTest() {
    when(repository.findLatestTransactionAuditEntityByDcbTransactionId(any())).thenReturn(Optional.empty());
    transactionAuditService.logErrorIfTransactionAuditNotExists(
      DCB_TRANSACTION_ID, createDcbTransactionByRole(LENDER), "error_message");
    verify(repository).save(any());
  }

  @Test
  void logErrorIfTransactionAuditNotExists_positive_duplicateErrorOnExistingEntity() {
    // TestMate-0f75eabf529985a5592a4c0d84c761b3
    var errorMsg = "duplicate detected";
    var dcbTransaction = borrowerDcbTransaction();
    var searchResult = Optional.of(new TransactionAuditEntity());
    var auditCaptor = ArgumentCaptor.forClass(TransactionAuditEntity.class);
    when(repository.findLatestTransactionAuditEntityByDcbTransactionId(DCB_TRANSACTION_ID)).thenReturn(searchResult);
    when(repository.save(auditCaptor.capture())).then(inv -> inv.getArgument(0));

    transactionAuditService.logErrorIfTransactionAuditNotExists(DCB_TRANSACTION_ID, dcbTransaction, errorMsg);

    var savedAudit = auditCaptor.getValue();
    assertThat(savedAudit.getAction()).isEqualTo("DUPLICATE_ERROR");
    assertThat(savedAudit.getTransactionId()).isEqualTo("-1");
    assertThat(savedAudit.getErrorMessage())
      .contains(DCB_TRANSACTION_ID)
      .contains("BORROWER")
      .contains(errorMsg);
  }

  @Test
  void logErrorIfTransactionAuditNotExists_positive_transactionObjectIsNull() {
    // TestMate-c6ffce49ce9c7a9bf08e4766a30a7e62
    var errorMsg = "null object error";
    var auditCaptor = ArgumentCaptor.forClass(TransactionAuditEntity.class);
    var searchResult = Optional.<TransactionAuditEntity>empty();
    when(repository.findLatestTransactionAuditEntityByDcbTransactionId(DCB_TRANSACTION_ID)).thenReturn(searchResult);
    when(repository.save(auditCaptor.capture())).then(inv -> inv.getArgument(0));

    transactionAuditService.logErrorIfTransactionAuditNotExists(DCB_TRANSACTION_ID, null, errorMsg);

    verify(repository).save(auditCaptor.capture());
    var savedAudit = auditCaptor.getValue();
    assertThat(savedAudit.getTransactionId()).isEqualTo(DCB_TRANSACTION_ID);
    assertThat(savedAudit.getAction()).isEqualTo("ERROR");
    assertThat(savedAudit.getBefore()).isNull();
    assertThat(savedAudit.getAfter()).isNull();
    assertThat(savedAudit.getErrorMessage())
      .contains("dcbTransactionId = %s".formatted(DCB_TRANSACTION_ID))
      .contains("role = null")
      .contains("error message = null object error");
  }
}
