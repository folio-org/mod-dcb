package org.folio.dcb.controller;

import io.swagger.v3.oas.annotations.Parameter;
import org.folio.dcb.rest.resource.TransactionsApi;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionApiController implements TransactionsApi {

  @Override
  public ResponseEntity<TransactionStatusResponse> getTransactionStatus(
    @Parameter(name = "dcbTransactionId", description = "The ReShare DCB ID for the transaction being brokered.") @PathVariable("dcbTransactionId") String dcbTransactionId
  ) {
    TransactionStatusResponse response = new TransactionStatusResponse();
    response.status(TransactionStatusResponse.StatusEnum.AWAITING_PICKUP);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
