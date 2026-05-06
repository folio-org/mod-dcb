package org.folio.dcb.controller;

import org.folio.dcb.service.EcsRequestTransactionsService;
import org.folio.dcb.service.TransactionAuditService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class EcsRequestTransactionsApiControllerTest {

  @InjectMocks
  private EcsRequestTransactionsApiController ecsRequestTransactionsApiController;

  @Mock
  private EcsRequestTransactionsService ecsRequestTransactionsService;

  @Mock
  private TransactionAuditService transactionAuditService;

    @Test
  void createEcsRequestTransactionsShouldReturnCreatedStatusWhenSuccessful() {
    // TestMate-fb0e3f3afb8bd45bf92f0bfdaf01a130
    // Given
    String ecsRequestTransactionId = "550e8400-e29b-41d4-a716-446655440000";
    DcbTransaction dcbTransaction = new DcbTransaction();
    dcbTransaction.setRole(DcbTransaction.RoleEnum.LENDER);
    TransactionStatusResponse expectedResponse = new TransactionStatusResponse();
    expectedResponse.setStatus(TransactionStatusResponse.StatusEnum.CREATED);
    when(ecsRequestTransactionsService.createEcsRequestTransactions(ecsRequestTransactionId, dcbTransaction))
      .thenReturn(expectedResponse);
    // When
    ResponseEntity<TransactionStatusResponse> actualResponse = ecsRequestTransactionsApiController
      .createEcsRequestTransactions(ecsRequestTransactionId, dcbTransaction);
    // Then
    assertEquals(HttpStatus.CREATED, actualResponse.getStatusCode());
    assertEquals(expectedResponse, actualResponse.getBody());
    verify(ecsRequestTransactionsService).createEcsRequestTransactions(ecsRequestTransactionId, dcbTransaction);
  }

    @Test
  void createEcsRequestTransactionsShouldLogErrorToAuditAndRethrowWhenServiceFails() {
    // TestMate-2b1511e07057750f28e1b2d54902dfe4
    // Given
    String ecsRequestTransactionId = "550e8400-e29b-41d4-a716-446655440000";
    DcbTransaction dcbTransaction = new DcbTransaction();
    dcbTransaction.setRole(DcbTransaction.RoleEnum.LENDER);
    String errorMessage = "Service failure";
    RuntimeException expectedException = new RuntimeException(errorMessage);
    when(ecsRequestTransactionsService.createEcsRequestTransactions(ecsRequestTransactionId, dcbTransaction))
      .thenThrow(expectedException);
    // When
    RuntimeException actualException = assertThrows(RuntimeException.class, () ->
      ecsRequestTransactionsApiController.createEcsRequestTransactions(ecsRequestTransactionId, dcbTransaction));
    // Then
    assertEquals(errorMessage, actualException.getMessage());
    verify(transactionAuditService).logErrorIfTransactionAuditNotExists(ecsRequestTransactionId, dcbTransaction, errorMessage);
    verify(ecsRequestTransactionsService).createEcsRequestTransactions(ecsRequestTransactionId, dcbTransaction);
  }

}
