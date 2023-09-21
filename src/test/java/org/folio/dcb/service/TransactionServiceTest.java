package org.folio.dcb.service;

import org.folio.dcb.service.impl.LendingLibraryServiceImpl;
import org.folio.dcb.service.impl.TransactionsServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.createDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.createTransactionResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class TransactionServiceTest {

  @InjectMocks
  TransactionsServiceImpl transactionsService;

  @Mock(name="lendingLibraryService")
  LendingLibraryServiceImpl lendingLibraryService;

  @Test
  void createCirculationRequestTest() {
    when(lendingLibraryService.createTransaction(any(), any()))
      .thenReturn(createTransactionResponse());
    transactionsService.createCirculationRequest(DCB_TRANSACTION_ID, createDcbTransaction());
    verify(lendingLibraryService).createTransaction(DCB_TRANSACTION_ID, createDcbTransaction());
  }
}
