package org.folio.dcb.exception;

/**
 * Exception thrown when a service operation fails.
 *
 * <p>This is a general-purpose exception thrown when business logic or service operations fail.
 * It can represent various error conditions within the service layer. The exception can be created with just a message
 * or with both a message and underlying cause for error chaining.
 */
public class ServiceException extends RuntimeException {

  /**
   * Constructs a new ServiceException with the specified error message.
   *
   * @param message the detail message describing the service error
   */
  public ServiceException(String message) {
    super(message);
  }

  /**
   * Creates {@link ServiceException} with error message and error cause.
   *
   * @param message the detail message describing the service error
   * @param cause the underlying cause of the exception
   */
  public ServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
