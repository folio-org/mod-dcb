package org.folio.dcb.exception;

public class InventoryResourceOperationException extends RuntimeException {

  public InventoryResourceOperationException(String message, Throwable error) {
    super(message, error);
  }

  public static InventoryResourceOperationException createInventoryResourceException(String resource, Throwable error) {
    return new InventoryResourceOperationException("Failed to create %s".formatted(resource), error);
  }

}
