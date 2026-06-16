package org.folio.dcb.exception;

/**
 * Exception thrown when an operation is not authorized.
 *
 * <p>This exception indicates that an operation or resource access is denied due to
 * authorization failure. It is typically associated with authentication or authorization checks (HTTP 401/403) and can
 * wrap an underlying cause exception for additional context.
 */
public class UnauthorizedException extends RuntimeException {

  /**
   * Creates {@link UnauthorizedException} with error message and error cause.
   *
   * @param message the detail message explaining the authorization failure
   * @param cause the underlying cause of the exception
   */
  public UnauthorizedException(String message, Throwable cause) {
    super(message, cause);
  }
}
