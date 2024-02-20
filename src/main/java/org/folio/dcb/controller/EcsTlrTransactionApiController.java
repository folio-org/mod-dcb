package org.folio.dcb.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.rest.resource.EcsTlrTransactionApi;
import org.folio.dcb.service.EcsTlrTransactionsService;
import org.folio.dcb.service.TransactionAuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
@RequiredArgsConstructor
public class EcsTlrTransactionApiController implements EcsTlrTransactionApi {

  private final EcsTlrTransactionsService ecsTlrTransactionsService;
  private final TransactionAuditService transactionAuditService;

  @Override
  public ResponseEntity<TransactionStatusResponse> createEcsTlrTransaction(String ecsTlrTransactionId, DcbTransaction dcbTransaction) {
    log.info("createEcsTlrTransaction:: creating ECS TLR Transaction {} with id {} ", dcbTransaction, ecsTlrTransactionId);
    TransactionStatusResponse transactionStatusResponse;
    try {
      transactionStatusResponse = ecsTlrTransactionsService.createEcsTlrTransaction(ecsTlrTransactionId, dcbTransaction);
    } catch (Exception ex) {
      transactionAuditService.logErrorIfTransactionAuditNotExists(ecsTlrTransactionId, dcbTransaction, ex.getMessage());
      throw ex;
    }
    return ResponseEntity.status(HttpStatus.CREATED)
      .body(transactionStatusResponse);
  }
}
