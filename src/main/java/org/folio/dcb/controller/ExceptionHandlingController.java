package org.folio.dcb.controller;

import feign.FeignException;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.spring.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.folio.dcb.utils.ErrorHelper.ErrorCode.BAD_GATEWAY;
import static org.folio.dcb.utils.ErrorHelper.ErrorCode.DUPLICATE_ERROR;
import static org.folio.dcb.utils.ErrorHelper.ErrorCode.NOT_FOUND_ERROR;
import static org.folio.dcb.utils.ErrorHelper.ErrorCode.VALIDATION_ERROR;
import static org.folio.dcb.utils.ErrorHelper.createExternalError;
import static org.folio.dcb.utils.ErrorHelper.createInternalError;

import org.folio.dcb.domain.dto.Errors;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Log4j2
public class ExceptionHandlingController {

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(NotFoundException.class)
  public Errors handleNotFoundException(NotFoundException ex) {
    logExceptionMessage(ex);
    return createExternalError(ex.getMessage(), NOT_FOUND_ERROR);
  }

  @ResponseStatus(HttpStatus.CONFLICT)
  @ExceptionHandler(ResourceAlreadyExistException.class)
  public Errors handleAlreadyExistException(ResourceAlreadyExistException ex) {
    logExceptionMessage(ex);
    return createExternalError(ex.getMessage(), DUPLICATE_ERROR);
  }

  @ResponseStatus(HttpStatus.BAD_GATEWAY)
  @ExceptionHandler(FeignException.BadGateway.class)
  public Errors handleBadGatewayException(FeignException.BadGateway ex) {
    logExceptionMessage(ex);
    return createInternalError(ex.getMessage(), BAD_GATEWAY);
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class
  })
  public Errors handleValidationErrors(Exception ex) {
    logExceptionMessage(ex);
    return createExternalError(ex.getMessage(), VALIDATION_ERROR);
  }

  private void logExceptionMessage(Exception ex) {
    log.warn("Exception occurred ", ex);
  }

}
