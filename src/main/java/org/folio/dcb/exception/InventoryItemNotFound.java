package org.folio.dcb.exception;

/**
 * Exception thrown when an inventory item is not found.
 *
 * <p>This exception is thrown when attempting to access or retrieve an inventory item that does
 * not exist in the inventory system. The exception uses modified suppression and stack trace behavior for efficient
 * error reporting.
 */
public class InventoryItemNotFound extends RuntimeException {

  /**
   * Constructs a new InventoryItemNotFound exception with the specified detail message.
   *
   * @param message the detail message describing which item was not found
   */
  public InventoryItemNotFound(String message) {
    super(message, null, true, false);
  }
}
