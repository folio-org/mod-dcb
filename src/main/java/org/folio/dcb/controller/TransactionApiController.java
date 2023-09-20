package org.folio.dcb.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.rest.resource.TransactionsApi;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.service.TransactionsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
@AllArgsConstructor
public class TransactionApiController implements TransactionsApi {

  private final TransactionsService transactionsService;
  @Override
  public ResponseEntity<TransactionStatusResponse> getTransactionStatus(
    @Parameter(name = "dcbTransactionId", description = "The ReShare DCB ID for the transaction being brokered.")
    @PathVariable("dcbTransactionId") String dcbTransactionId) {
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<TransactionStatusResponse> createCirculationRequest(String dcbTransactionId, DcbTransaction dcbTransaction) {
    log.info("createCirculationRequest:: creating dcbTransaction {} with id {} ", dcbTransaction, dcbTransactionId);
    return ResponseEntity.ok(transactionsService.createCirculationRequest(dcbTransactionId, dcbTransaction));
  }
}
