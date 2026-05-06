package org.folio.dcb.controller;

import org.folio.dcb.service.TransactionAuditService;
import org.folio.dcb.service.TransactionsService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.folio.dcb.controller.TransactionApiController;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class TransactionApiControllerTest {

  @InjectMocks
  private TransactionApiController transactionApiController;

  @Mock
  private TransactionsService transactionsService;

  @Mock
  private TransactionAuditService transactionAuditService;

    @Test
void createCirculationRequestTest() {
  // TestMate-4a274117d55982eb7ff0b5e267ec86ed
  // given
  String dcbTransactionId = "txn-123";
  DcbTransaction dcbTransaction = DcbTransaction.builder()
    .role(DcbTransaction.RoleEnum.LENDER)
    .build();
  TransactionStatusResponse expectedResponse = TransactionStatusResponse.builder()
    .status(TransactionStatusResponse.StatusEnum.CREATED)
    .build();
  when(transactionsService.createCirculationRequest(dcbTransactionId, dcbTransaction))
    .thenReturn(expectedResponse);
  // when
  ResponseEntity<TransactionStatusResponse> responseEntity = transactionApiController.createCirculationRequest(dcbTransactionId, dcbTransaction);
  // then
  assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  assertThat(responseEntity.getBody()).isEqualTo(expectedResponse);
  verify(transactionsService).createCirculationRequest(dcbTransactionId, dcbTransaction);
}

    @Test
  void createCirculationRequestShouldLogAuditAndThrowWhenExceptionOccurs() {
    // TestMate-515a8d0a1d8b2ffc0a8908e3322eb24e
    // given
    String dcbTransactionId = "txn-err-1";
    String errorMessage = "Service Error";
    DcbTransaction dcbTransaction = DcbTransaction.builder()
      .role(DcbTransaction.RoleEnum.LENDER)
      .build();
    RuntimeException exception = new RuntimeException(errorMessage);
    when(transactionsService.createCirculationRequest(dcbTransactionId, dcbTransaction))
      .thenThrow(exception);
    // when
    RuntimeException thrownException = assertThrows(RuntimeException.class, () ->
      transactionApiController.createCirculationRequest(dcbTransactionId, dcbTransaction));
    // then
    assertThat(thrownException.getMessage()).isEqualTo(errorMessage);
    verify(transactionsService).createCirculationRequest(dcbTransactionId, dcbTransaction);
    verify(transactionAuditService).logErrorIfTransactionAuditNotExists(dcbTransactionId, dcbTransaction, errorMessage);
  }

}
