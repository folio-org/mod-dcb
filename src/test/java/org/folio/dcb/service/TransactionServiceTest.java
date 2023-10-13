package org.folio.dcb.service;

import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.mapper.TransactionAuditMapper;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.LendingLibraryServiceImpl;
import org.folio.dcb.service.impl.TransactionsServiceImpl;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.utils.EntityUtils.*;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  @InjectMocks
  private TransactionsServiceImpl transactionsService;
  @Mock(name="lendingLibraryService")
  private LendingLibraryServiceImpl lendingLibraryService;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private TransactionMapper transactionMapper;
  @Mock
  private TransactionAuditMapper transactionAuditMapper;
  @Mock
  private TransactionsAuditService transactionsAuditService;

  @Test
  void createCirculationRequestTest() {
    TransactionAuditEntity transactionAuditEntity = TransactionAuditEntity.builder().transactionId("123").build();
    TransactionEntity transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);

    when(lendingLibraryService.createTransaction(any(), any())).thenReturn(createTransactionResponse());
    when(transactionAuditMapper.mapToEntity(any())).thenReturn(transactionAuditEntity);
    when(transactionRepository.findById(any())).thenReturn(Optional.ofNullable(transactionEntity));

    transactionsService.createCirculationRequest(DCB_TRANSACTION_ID, createDcbTransaction());

    verify(lendingLibraryService).createTransaction(DCB_TRANSACTION_ID, createDcbTransaction());
    verify(transactionsAuditService).createTransactionAuditRecord(any());
  }

  @Test
  void shouldReturnAnyTransactionStatusById(){
    var transactionIdUnique = UUID.randomUUID().toString();
    when(transactionRepository.findById(transactionIdUnique))
      .thenReturn(Optional.ofNullable(TransactionEntity.builder()
        .status(TransactionStatus.StatusEnum.CREATED)
        .role(LENDER)
        .build()));

    var trnInstance = transactionsService.getTransactionStatusById(transactionIdUnique);
    assertNotNull(trnInstance);
    assertEquals(TransactionStatusResponse.StatusEnum.CREATED, trnInstance.getStatus());
  }

  @Test
  void getTransactionStatusByIdNotFoundExceptionTest(){
    var transactionIdUnique = UUID.randomUUID().toString();
    when(transactionRepository.findById(transactionIdUnique))
      .thenReturn(Optional.empty());

    Throwable exception = assertThrows(
      NotFoundException.class, () -> transactionsService.getTransactionStatusById(transactionIdUnique)
    );

    Assertions.assertEquals(String.format("DCB Transaction was not found by id= %s ", transactionIdUnique), exception.getMessage());
  }
}
