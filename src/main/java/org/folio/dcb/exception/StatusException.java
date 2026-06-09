package org.folio.dcb.exception;

/**
 * Exception thrown when an error occurs during status-related operations.
 *
 * <p>This exception is thrown when a status operation (such as status validation, update, or
 * verification) fails or encounters an invalid state. It serves as a general-purpose exception for status-related
 * errors in the system.
 */
public class StatusException extends RuntimeException {

  /**
   * Constructs a new StatusException with the specified error message.
   *
   * @param errorMsg the detail message describing the status error
   */
  public StatusException(String errorMsg) {
    super(errorMsg);
  }
}
