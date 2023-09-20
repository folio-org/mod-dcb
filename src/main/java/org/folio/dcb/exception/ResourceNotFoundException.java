package org.folio.dcb.exception;

public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String errorMsg) {
    super(errorMsg);
  }

}
