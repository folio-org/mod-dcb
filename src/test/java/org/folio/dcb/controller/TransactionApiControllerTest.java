package org.folio.dcb.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.dcb.utils.EntityUtils.lenderDcbTransaction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.dto.TransactionStatusResponse.StatusEnum;
import org.folio.dcb.service.TransactionAuditService;
import org.folio.dcb.service.TransactionsService;
import org.folio.dcb.support.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TransactionApiControllerTest {

  @InjectMocks private TransactionApiController transactionApiController;
  @Mock private TransactionsService transactionsService;
  @Mock private TransactionAuditService transactionAuditService;

  @Test
  void createCirculationRequestTest() {
    // TestMate-fb0e3f3afb8bd45bf92f0bfdaf01a130
    var dcbTransactionId = "txn-123";
    var dcbTransaction = lenderDcbTransaction();
    var expectedResponse = new TransactionStatusResponse().status(StatusEnum.CREATED);
    when(transactionsService.createCirculationRequest(dcbTransactionId, dcbTransaction)).thenReturn(expectedResponse);

    var responseEntity = transactionApiController.createCirculationRequest(dcbTransactionId, dcbTransaction);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(responseEntity.getBody()).isEqualTo(expectedResponse);
    verify(transactionsService).createCirculationRequest(dcbTransactionId, dcbTransaction);
  }

  @Test
  void createCirculationRequestShouldLogAuditAndThrowWhenExceptionOccurs() {
    // TestMate-515a8d0a1d8b2ffc0a8908e3322eb24e
    var dcbTransactionId = UUID.randomUUID().toString();
    var errorMessage = "Service Error";
    var dcbTransaction = lenderDcbTransaction();
    var exception = new RuntimeException(errorMessage);
    when(transactionsService.createCirculationRequest(dcbTransactionId, dcbTransaction)).thenThrow(exception);

    assertThatThrownBy(() ->
      transactionApiController.createCirculationRequest(dcbTransactionId, dcbTransaction))
      .isInstanceOf(RuntimeException.class)
      .hasMessage(errorMessage);
    verify(transactionsService).createCirculationRequest(dcbTransactionId, dcbTransaction);
    verify(transactionAuditService).logErrorIfTransactionAuditNotExists(dcbTransactionId, dcbTransaction, errorMessage);
  }

  @Test
  void getTransactionStatusByIdTest() {
    // TestMate-5265ce147e6303a6c6059ec4eb249b3a
    var dcbTransactionId = UUID.randomUUID().toString();
    var expectedResponse = new TransactionStatusResponse().status(StatusEnum.CREATED);
    when(transactionsService.getTransactionStatusById(dcbTransactionId)).thenReturn(expectedResponse);

    var responseEntity = transactionApiController.getTransactionStatusById(dcbTransactionId);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseEntity.getBody()).isEqualTo(expectedResponse);
    verify(transactionsService).getTransactionStatusById(dcbTransactionId);
  }

  @Test
  void getTransactionStatusByIdShouldLogAuditAndThrowWhenExceptionOccurs() {
    // TestMate-340f043d8cb1572c0d7d557ce271239c
    var dcbTransactionId = UUID.randomUUID().toString();
    var errorMessage = "Database connection failed";
    var exception = new RuntimeException(errorMessage);
    when(transactionsService.getTransactionStatusById(dcbTransactionId)).thenThrow(exception);

    assertThatThrownBy(() -> transactionApiController.getTransactionStatusById(dcbTransactionId))
      .isInstanceOf(RuntimeException.class)
      .hasMessage(errorMessage);
    verify(transactionsService).getTransactionStatusById(dcbTransactionId);
    verify(transactionAuditService).logErrorIfTransactionAuditExists(dcbTransactionId, errorMessage);
  }
}
