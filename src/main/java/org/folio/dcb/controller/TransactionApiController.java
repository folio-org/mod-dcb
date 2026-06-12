package org.folio.dcb.controller;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.DcbUpdateTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.dto.TransactionStatusResponseCollection;
import org.folio.dcb.rest.resource.TransactionsApi;
import org.folio.dcb.service.TransactionAuditService;
import org.folio.dcb.service.TransactionsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@AllArgsConstructor
public class TransactionApiController implements TransactionsApi {

  private final TransactionsService transactionsService;
  private final TransactionAuditService transactionAuditService;

  @Override
  public ResponseEntity<TransactionStatusResponse> renewItemLoanByTransactionId(String dcbTransactionId) {
    var statusResponse = transactionsService.renewLoanByTransactionId(dcbTransactionId);
    return ResponseEntity.status(HttpStatus.OK).body(statusResponse);
  }

  @Override
  public ResponseEntity<TransactionStatusResponse> getTransactionStatusById(String dcbTransactionId) {
    log.debug("getTransactionStatus:: by id {} ", dcbTransactionId);
    try {
      var transactionStatusResponse = transactionsService.getTransactionStatusById(dcbTransactionId);
      return ResponseEntity.status(HttpStatus.OK).body(transactionStatusResponse);
    } catch (Exception ex) {
      transactionAuditService.logErrorIfTransactionAuditExists(dcbTransactionId, ex.getMessage());
      throw ex;
    }
  }

  @Override
  public ResponseEntity<TransactionStatusResponse> createCirculationRequest(String id, DcbTransaction dcbTransaction) {
    log.info("createCirculationRequest:: creating transaction {} for role {} ", id, dcbTransaction.getRole());
    try {
      var transactionStatusResponse = transactionsService.createCirculationRequest(id, dcbTransaction);
      return ResponseEntity.status(HttpStatus.CREATED).body(transactionStatusResponse);
    } catch (Exception ex) {
      transactionAuditService.logErrorIfTransactionAuditNotExists(id, dcbTransaction, ex.getMessage());
      throw ex;
    }
  }

  @Override
  public ResponseEntity<TransactionStatusResponse> updateTransactionStatus(String id, TransactionStatus status) {
    log.info("updateTransactionStatus:: updating dcbTransaction with id {} to status {} ", id, status.getStatus());
    log.info("updateTransactionStatus:: status={}", status);
    try {
      var transactionStatusResponse = transactionsService.updateTransactionStatus(id, status);
      return ResponseEntity.status(HttpStatus.OK).body(transactionStatusResponse);
    } catch (Exception ex) {
      transactionAuditService.logErrorIfTransactionAuditExists(id, ex.getMessage());
      throw ex;
    }
  }

  @Override
  public ResponseEntity<TransactionStatusResponseCollection> getTransactionStatusList(
    OffsetDateTime fromDate, OffsetDateTime toDate, Integer pageNumber, Integer pageSize) {

    log.debug("getTransactionStatusList:: fetching transaction lists with fromDate {}, toDate {},"
      + " pageNumber {}, pageSize {}", fromDate, toDate, pageNumber, pageSize);
    var transactionStatusList = transactionsService.getTransactionStatusList(fromDate, toDate, pageNumber, pageSize);
    return ResponseEntity.status(HttpStatus.OK).body(transactionStatusList);
  }

  @Override
  public ResponseEntity<Void> updateTransactionDetails(String id, DcbUpdateTransaction dcbUpdateTransaction) {
    transactionsService.updateTransactionDetails(id, dcbUpdateTransaction);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Void> blockItemRenewalByTransactionId(String dcbTransactionId) {
    transactionsService.blockItemRenewalByTransactionId(dcbTransactionId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Void> unblockItemRenewalByTransactionId(String dcbTransactionId) {
    transactionsService.unblockItemRenewalByTransactionId(dcbTransactionId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
