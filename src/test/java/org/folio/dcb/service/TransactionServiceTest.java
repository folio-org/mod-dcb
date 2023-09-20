package org.folio.dcb.service;

import org.folio.dcb.domain.entity.Transactions;
import org.folio.dcb.repository.TransactionsRepository;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
class TransactionServiceTest {
  @MockBean
  private TransactionsRepository transactionsRepository;
  @Autowired
  private TransactionService transactionService;

  @Test
  public void shouldReturnAnyTransactionStatusById(){
    var transactionIdUnique = UUID.randomUUID();
    when(transactionsRepository.findById(transactionIdUnique))
      .thenReturn(Optional.of(new Transactions()));

    var trnInstance = transactionService.getTransactionStatusById(transactionIdUnique);
    assertNotNull(trnInstance);
  }

  @Test
  public void transactionStatusByIdNotFoundExceptionTest(){
    var transactionIdUnique = UUID.randomUUID();
    when(transactionsRepository.findById(transactionIdUnique))
      .thenReturn(Optional.empty());

    Throwable exception = assertThrows(
      NotFoundException.class, () -> transactionService.getTransactionStatusById(transactionIdUnique)
    );

    assertEquals("DCB Transaction was not found by id=" + transactionIdUnique, exception.getMessage());
  }
}
