package org.folio.dcb.exception;

/**
 * Exception thrown when an inventory resource operation fails.
 *
 * <p>This exception is thrown when operations on inventory resources, such as creation, update, or
 * retrieval, encounter failures. It typically wraps the underlying cause exception to provide context about the
 * failure.
 */
public class InventoryResourceOperationException extends RuntimeException {

  /**
   * Constructs a new InventoryResourceOperationException with the specified message and cause.
   *
   * @param message the detail message describing the error
   * @param error the underlying cause of the exception
   */
  public InventoryResourceOperationException(String message, Throwable error) {
    super(message, error);
  }

  /**
   * Factory method to create an InventoryResourceOperationException for a failed resource creation.
   *
   * @param resource the name of the resource that failed to be created
   * @param error the underlying cause of the exception
   * @return a new InventoryResourceOperationException with a formatted message
   */
  public static InventoryResourceOperationException createInventoryResourceException(String resource, Throwable error) {
    return new InventoryResourceOperationException("Failed to create %s".formatted(resource), error);
  }
}
