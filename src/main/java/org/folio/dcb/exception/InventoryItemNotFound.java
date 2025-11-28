package org.folio.dcb.exception;

public class InventoryItemNotFound extends RuntimeException {

  /**
   * Constructs a new InventoryItemNotFound exception with the specified detail message.
   *
   * @param message the detail message
   */
  public InventoryItemNotFound(String message) {
    super(message, null, true, false);
  }
}
