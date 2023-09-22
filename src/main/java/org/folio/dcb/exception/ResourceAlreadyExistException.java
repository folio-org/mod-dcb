package org.folio.dcb.exception;

public class ResourceAlreadyExistException extends RuntimeException {

  public ResourceAlreadyExistException(String errorMsg) {
    super(errorMsg);
  }

}
