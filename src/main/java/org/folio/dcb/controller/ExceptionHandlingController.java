package org.folio.dcb.controller;

import static org.folio.dcb.utils.ErrorHelper.ErrorCode.BAD_GATEWAY;
import static org.folio.dcb.utils.ErrorHelper.ErrorCode.DUPLICATE_ERROR;
import static org.folio.dcb.utils.ErrorHelper.ErrorCode.INTERNAL_SERVER_ERROR;
import static org.folio.dcb.utils.ErrorHelper.ErrorCode.NOT_FOUND_ERROR;
import static org.folio.dcb.utils.ErrorHelper.ErrorCode.VALIDATION_ERROR;
import static org.folio.dcb.utils.ErrorHelper.createExternalError;
import static org.folio.dcb.utils.ErrorHelper.createInternalError;

import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.Errors;
import org.folio.dcb.exception.DcbHubLocationException;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.exception.StatusException;
import org.folio.spring.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception handler for REST endpoints.
 *
 * <p>This controller advice intercepts and handles exceptions thrown by REST controllers,
 * converting them into appropriate HTTP responses with error details. It provides
 * centralized exception handling across the application.</p>
 *
 * <p>Each handler method returns an {@link Errors} DTO containing error code and message,
 * and sets the appropriate HTTP status code.</p>
 */
@Log4j2
@RestControllerAdvice
public class ExceptionHandlingController {

  /**
   * Handles all uncaught exceptions that are not handled by specific handlers.
   *
   * @param ex the exception that was thrown
   * @return an {@link Errors} object with internal server error details
   * @since 1.0
   */
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public Errors handleGlobalException(Exception ex) {
    logExceptionMessage(ex);
    return createExternalError(ex.getMessage(), INTERNAL_SERVER_ERROR);
  }

  /**
   * Handles not found exceptions.
   *
   * <p>This method handles both FOLIO-specific {@link NotFoundException} and Spring's
   * {@link HttpClientErrorException.NotFound} exceptions.</p>
   *
   * @param ex the not found exception
   * @return an {@link Errors} object with not found error details
   */
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler({
    NotFoundException.class,
    HttpClientErrorException.NotFound.class
  })
  public Errors handleNotFoundException(Exception ex) {
    logExceptionMessage(ex);
    return createExternalError(ex.getMessage(), NOT_FOUND_ERROR);
  }

  /**
   * Handles resource already exists (conflict) exceptions.
   *
   * <p>This method handles both custom {@link ResourceAlreadyExistException} and Spring's
   * {@link HttpClientErrorException.Conflict} exceptions.</p>
   *
   * @param ex the conflict exception
   * @return an {@link Errors} object with duplicate error details
   */
  @ResponseStatus(HttpStatus.CONFLICT)
  @ExceptionHandler({
    ResourceAlreadyExistException.class,
    HttpClientErrorException.Conflict.class
  })
  public Errors handleAlreadyExistException(Exception ex) {
    logExceptionMessage(ex);
    return createExternalError(ex.getMessage(), DUPLICATE_ERROR);
  }

  /**
   * Handles unprocessable content (entity) exceptions.
   *
   * <p>This method handles validation errors that result in unprocessable content,
   * typically from HTTP 422 responses.</p>
   *
   * @param ex the unprocessable content exception
   * @return an {@link Errors} object with validation error details
   */
  @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
  @ExceptionHandler(HttpClientErrorException.UnprocessableContent.class)
  public Errors handleUnProcessableEntityErrors(Exception ex) {
    logExceptionMessage(ex);
    return createExternalError(ex.getMessage(), VALIDATION_ERROR);
  }

  /**
   * Handles bad gateway exceptions.
   *
   * <p>This method handles HTTP 502 Bad Gateway errors, typically occurring when
   * an upstream service is unreachable or returns an invalid response.</p>
   *
   * @param ex the bad gateway exception
   * @return an {@link Errors} object with bad gateway error details
   */
  @ResponseStatus(HttpStatus.BAD_GATEWAY)
  @ExceptionHandler(HttpServerErrorException.BadGateway.class)
  public Errors handleBadGatewayException(HttpServerErrorException.BadGateway ex) {
    logExceptionMessage(ex);
    return createInternalError(ex.getMessage(), BAD_GATEWAY);
  }

  /**
   * Handles validation and bad request errors.
   *
   * <p>This method handles multiple validation-related exceptions including:
   * <ul>
   *   <li>Missing servlet request parameters</li>
   *   <li>Method argument type mismatches</li>
   *   <li>HTTP message parsing errors</li>
   *   <li>Illegal argument errors</li>
   *   <li>Status exceptions</li>
   *   <li>HTTP 400 Bad Request errors</li>
   *   <li>Method argument validation errors</li>
   * </ul>
   * </p>
   *
   * @param ex the validation or bad request exception
   * @return an {@link Errors} object with validation error details
   */
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class,
    StatusException.class,
    HttpClientErrorException.BadRequest.class,
    MethodArgumentNotValidException.class
  })
  public Errors handleValidationErrors(Exception ex) {
    logExceptionMessage(ex);
    return createExternalError(ex.getMessage(), VALIDATION_ERROR);
  }

  /**
   * Handles DCB Hub location-specific exceptions.
   *
   * <p>This method handles {@link DcbHubLocationException} which contains
   * custom HTTP status information and error details.</p>
   *
   * @param ex the DCB hub location exception
   * @return a {@link ResponseEntity} with the appropriate HTTP status and error details
   */
  @ExceptionHandler(DcbHubLocationException.class)
  public ResponseEntity<Errors> handleDcbHubLocationException(DcbHubLocationException ex) {
    logExceptionMessage(ex);
    return ResponseEntity.status(ex.getHttpStatus())
      .body(createInternalError(ex.getMessage(), null));
  }

  private void logExceptionMessage(Exception ex) {
    log.warn("Exception occurred [{}]", ex.getClass().getSimpleName(), ex);
  }
}
