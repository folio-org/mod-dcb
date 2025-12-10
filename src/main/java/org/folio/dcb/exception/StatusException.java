package org.folio.dcb.exception;

public class StatusException extends RuntimeException{

  public StatusException(String errorMsg) {
    super(errorMsg);
  }

  public StatusException(String errorMsg, Throwable cause) {
    super(errorMsg, cause);
  }
}
