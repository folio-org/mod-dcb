package org.folio.dcb.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.dcb.utils.EntityUtils.lenderDcbTransaction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.service.EcsRequestTransactionsService;
import org.folio.dcb.service.TransactionAuditService;
import org.folio.dcb.support.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@UnitTest
@ExtendWith(MockitoExtension.class)
class EcsRequestTransactionsApiControllerTest {

  @InjectMocks private EcsRequestTransactionsApiController ecsRequestTransactionsApiController;
  @Mock private EcsRequestTransactionsService ecsRequestTransactionsService;
  @Mock private TransactionAuditService transactionAuditService;

  @Test
  void createEcsRequestTransactionsShouldReturnCreatedStatusWhenSuccessful() {
    // TestMate-fb0e3f3afb8bd45bf92f0bfdaf01a130
    var txId = UUID.randomUUID().toString();
    var dcbTransaction = lenderDcbTransaction();
    var expectedResponse = new TransactionStatusResponse().status(TransactionStatusResponse.StatusEnum.CREATED);
    when(ecsRequestTransactionsService.createEcsRequestTransactions(txId, dcbTransaction)).thenReturn(expectedResponse);

    var actualResponse = ecsRequestTransactionsApiController.createEcsRequestTransactions(txId, dcbTransaction);

    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(actualResponse.getBody()).isEqualTo(expectedResponse);
    verify(ecsRequestTransactionsService).createEcsRequestTransactions(txId, dcbTransaction);
  }

  @Test
  void createEcsRequestTransactionsShouldLogErrorToAuditAndRethrowWhenServiceFails() {
    // TestMate-2b1511e07057750f28e1b2d54902dfe4
    var transactionId = UUID.randomUUID().toString();
    var dcbTransaction = lenderDcbTransaction();
    var errorMessage = "Service failure";
    when(ecsRequestTransactionsService.createEcsRequestTransactions(transactionId, dcbTransaction))
      .thenThrow(new RuntimeException(errorMessage));

    assertThatThrownBy(() ->
      ecsRequestTransactionsApiController.createEcsRequestTransactions(transactionId, dcbTransaction))
      .isInstanceOf(RuntimeException.class)
      .hasMessage(errorMessage);

    verify(transactionAuditService).logErrorIfTransactionAuditNotExists(transactionId, dcbTransaction, errorMessage);
    verify(ecsRequestTransactionsService).createEcsRequestTransactions(transactionId, dcbTransaction);
  }

  @Test
  void updateEcsRequestTransactionShouldReturnOkStatusWhenSuccessful() {
    // TestMate-b3c9d28477affe116c99ccaaa62a5265
    var txId = UUID.randomUUID().toString();
    var dcbTransaction = lenderDcbTransaction();
    var expectedResponse = new TransactionStatusResponse().status(TransactionStatusResponse.StatusEnum.OPEN);
    when(ecsRequestTransactionsService.updateEcsRequestTransaction(txId, dcbTransaction))
      .thenReturn(expectedResponse);

    var actualResponse = ecsRequestTransactionsApiController.updateEcsRequestTransaction(txId, dcbTransaction);

    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actualResponse.getBody()).isEqualTo(expectedResponse);
    verify(ecsRequestTransactionsService).updateEcsRequestTransaction(txId, dcbTransaction);
  }

  @Test
  void updateEcsRequestTransactionShouldLogErrorToAuditAndRethrowWhenServiceFails() {
    // TestMate-bc4856a5a28a7384fc616339818b9260
    var txId = UUID.randomUUID().toString();
    var dcbTransaction = lenderDcbTransaction();
    var errorMessage = "Update failed";
    when(ecsRequestTransactionsService.updateEcsRequestTransaction(txId, dcbTransaction))
      .thenThrow(new RuntimeException(errorMessage));

    assertThatThrownBy(() -> ecsRequestTransactionsApiController.updateEcsRequestTransaction(txId, dcbTransaction))
      .isInstanceOf(RuntimeException.class)
      .hasMessage(errorMessage);

    verify(transactionAuditService).logErrorIfTransactionAuditNotExists(txId, dcbTransaction, errorMessage);
    verify(ecsRequestTransactionsService).updateEcsRequestTransaction(txId, dcbTransaction);
  }
}
