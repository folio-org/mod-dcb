package org.folio.dcb.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown for errors related to DCB hub location operations.
 *
 * <p>This exception indicates an error occurred during DCB (Distributed Community Bookshare) hub
 * location operations. It encapsulates an HTTP status code to allow callers to determine the appropriate HTTP response
 * status.
 */
@Getter
public class DcbHubLocationException extends RuntimeException {

  /**
   * The HTTP status code associated with this exception.
   */
  private final HttpStatus httpStatus;

  /**
   * Constructs a new DcbHubLocationException with the specified message and HTTP status.
   *
   * @param message the detail message describing the error
   * @param httpStatus the HTTP status code associated with this error
   */
  public DcbHubLocationException(String message, HttpStatus httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
  }

  /**
   * Constructs a new DcbHubLocationException with the specified message, HTTP status, and cause.
   *
   * @param message the detail message describing the error
   * @param httpStatus the HTTP status code associated with this error
   * @param cause the underlying cause of the exception
   */
  public DcbHubLocationException(String message, HttpStatus httpStatus, Throwable cause) {
    super(message, cause);
    this.httpStatus = httpStatus;
  }
}
