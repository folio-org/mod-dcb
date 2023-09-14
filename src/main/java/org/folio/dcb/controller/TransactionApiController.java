package org.folio.dcb.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.mapper.TransactionStatusMapper;
import org.folio.dcb.rest.resource.TransactionsApi;
import org.folio.dcb.domain.dto.TransactionStatusDto;
import org.folio.dcb.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Log4j2
public class TransactionApiController implements TransactionsApi {
  private final TransactionService transactionService;
  private final TransactionStatusMapper transactionStatusMapper;

  @Override
  public ResponseEntity<TransactionStatusDto> getTransactionStatus(
    @Parameter(name = "dcbTransactionId", description = "The ReShare DCB ID for the transaction being brokered.")
    @PathVariable("dcbTransactionId") UUID dcbTransactionId) {
    return new ResponseEntity<>(transactionStatusMapper.mapToDto(transactionService.getTransactionStatusById(dcbTransactionId)), HttpStatus.OK);
  }
}
