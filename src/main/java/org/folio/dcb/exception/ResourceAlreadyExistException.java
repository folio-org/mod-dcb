package org.folio.dcb.exception;

/**
 * Exception thrown when an attempt is made to create a resource that already exists.
 *
 * <p>This exception indicates that a resource creation operation failed because a resource with
 * the same identifier or unique properties already exists in the system. This is typically associated with a conflict
 * (HTTP 409) error.
 */
public class ResourceAlreadyExistException extends RuntimeException {

  /**
   * Constructs a new ResourceAlreadyExistException with the specified error message.
   *
   * @param errorMsg the detail message describing the conflict
   */
  public ResourceAlreadyExistException(String errorMsg) {
    super(errorMsg);
  }
}
