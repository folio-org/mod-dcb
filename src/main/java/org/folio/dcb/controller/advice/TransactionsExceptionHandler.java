package org.folio.dcb.controller.advice;

import org.folio.spring.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class TransactionsExceptionHandler {
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<String> handleNotFoundException(final NotFoundException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
  }
}
