package org.folio.dcb.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.rest.resource.EcsRequestTransactionsApi;
import org.folio.dcb.service.EcsRequestTransactionsService;
import org.folio.dcb.service.TransactionAuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
@RequiredArgsConstructor
public class EcsRequestTransactionsApiController implements EcsRequestTransactionsApi {

  private final EcsRequestTransactionsService ecsRequestTransactionsService;
  private final TransactionAuditService transactionAuditService;

  @Override
  public ResponseEntity<TransactionStatusResponse> createEcsRequestTransactions(
    String ecsRequestTransactionId, DcbTransaction dcbTransaction) {

    log.info("createEcsRequestTransactions:: creating ECS Request Transaction {} with ID {}",
      dcbTransaction, ecsRequestTransactionId);
    TransactionStatusResponse transactionStatusResponse;
    try {
      transactionStatusResponse = ecsRequestTransactionsService.createEcsRequestTransactions(
        ecsRequestTransactionId, dcbTransaction);
    } catch (Exception ex) {
      transactionAuditService.logErrorIfTransactionAuditNotExists(ecsRequestTransactionId,
        dcbTransaction, ex.getMessage());
      throw ex;
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(transactionStatusResponse);
  }

  @Override
  public ResponseEntity<TransactionStatusResponse> updateEcsRequestTransaction(
    String ecsRequestTransactionId, DcbTransaction dcbTransaction) {

    log.info("updateEcsRequestTransaction:: update ECS request transaction {}",
      ecsRequestTransactionId);
    TransactionStatusResponse transactionStatusResponse;
    try {
      transactionStatusResponse = ecsRequestTransactionsService.updateEcsRequestTransaction(
        ecsRequestTransactionId, dcbTransaction);
    } catch (Exception ex) {
      transactionAuditService.logErrorIfTransactionAuditNotExists(ecsRequestTransactionId,
        dcbTransaction, ex.getMessage());
      throw ex;
    }
    return ResponseEntity.status(HttpStatus.OK).body(transactionStatusResponse);
  }
}
