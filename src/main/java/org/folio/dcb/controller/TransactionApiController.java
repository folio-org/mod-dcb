package org.folio.dcb.controller;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.DcbUpdateTransaction;
import org.folio.dcb.domain.dto.ServicePointExpirationPeriod;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponseCollection;
import org.folio.dcb.domain.entity.ServicePointExpirationPeriodEntity;
import org.folio.dcb.repository.ServicePointExpirationPeriodRepository;
import org.folio.dcb.rest.resource.TransactionsApi;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.service.TransactionAuditService;
import org.folio.dcb.service.TransactionsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.time.OffsetDateTime;
import java.util.Objects;

@RestController
@Log4j2
@AllArgsConstructor
public class TransactionApiController implements TransactionsApi {

  private final TransactionsService transactionsService;
  private final TransactionAuditService transactionAuditService;
  private final ServicePointExpirationPeriodRepository servicePointExpirationPeriodRepository;

  @Override
  public ResponseEntity<ServicePointExpirationPeriod> getSpPeriod() {
    ServicePointExpirationPeriodEntity sp = servicePointExpirationPeriodRepository.findAll()
      .stream()
      .findFirst()
      .orElseGet(() -> ServicePointExpirationPeriodEntity.builder().build());
String id = Objects.isNull(sp.getId()) ? "" : sp.getId().toString();
    return ResponseEntity.status(HttpStatus.OK).body(ServicePointExpirationPeriod.builder()
      .id(id)
      .duration(sp.getDuration())
      .interval(sp.getIntervalId())
      .build());
  }

  @Override
  public ResponseEntity<TransactionStatusResponse> getTransactionStatusById(String dcbTransactionId) {
    log.info("getTransactionStatus:: by id {} ", dcbTransactionId);
    TransactionStatusResponse transactionStatusResponse;
    try {
      transactionStatusResponse = transactionsService.getTransactionStatusById(dcbTransactionId);
    } catch (Exception ex) {
      transactionAuditService.logErrorIfTransactionAuditExists(dcbTransactionId, ex.getMessage());
      throw ex;
    }

    return ResponseEntity.status(HttpStatus.OK)
      .body(transactionStatusResponse);
  }

  @Override
  public ResponseEntity<TransactionStatusResponse> createCirculationRequest(String dcbTransactionId, DcbTransaction dcbTransaction) {
    log.info("createCirculationRequest:: creating dcbTransaction {} with id {} ", dcbTransaction, dcbTransactionId);
    TransactionStatusResponse transactionStatusResponse;
    try {
      transactionStatusResponse = transactionsService.createCirculationRequest(dcbTransactionId, dcbTransaction);
    } catch (Exception ex) {
      transactionAuditService.logErrorIfTransactionAuditNotExists(dcbTransactionId, dcbTransaction, ex.getMessage());
      throw ex;
    }

    return ResponseEntity.status(HttpStatus.CREATED)
      .body(transactionStatusResponse);
  }

  @Override
  public ResponseEntity<TransactionStatusResponse> updateTransactionStatus(String dcbTransactionId, TransactionStatus transactionStatus) {
    log.info("updateTransactionStatus:: updating dcbTransaction with id {} to status {} ", dcbTransactionId, transactionStatus.getStatus());
    TransactionStatusResponse transactionStatusResponse;
    try {
      transactionStatusResponse = transactionsService.updateTransactionStatus(dcbTransactionId, transactionStatus);
    } catch (Exception ex) {
      transactionAuditService.logErrorIfTransactionAuditExists(dcbTransactionId, ex.getMessage());
      throw ex;
    }

    return ResponseEntity.status(HttpStatus.OK)
      .body(transactionStatusResponse);
  }

  @Override
  public ResponseEntity<TransactionStatusResponseCollection> getTransactionStatusList(OffsetDateTime fromDate, OffsetDateTime toDate, Integer pageNumber, Integer pageSize) {
    log.info("getTransactionStatusList:: fetching transaction lists with fromDate {}, toDate {}, pageNumber {}, pageSize {}",
      fromDate, toDate, pageNumber, pageSize);
    return ResponseEntity.status(HttpStatus.OK)
      .body(transactionsService.getTransactionStatusList(fromDate, toDate, pageNumber, pageSize));
  }

  @Override
  public ResponseEntity<Void> updateTransactionDetails(String dcbTransactionId, DcbUpdateTransaction dcbUpdateTransaction) {
    transactionsService.updateTransactionDetails(dcbTransactionId, dcbUpdateTransaction);
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
      .build();
  }

}
