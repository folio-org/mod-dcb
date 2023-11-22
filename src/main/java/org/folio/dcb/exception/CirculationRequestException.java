package org.folio.dcb.exception;

public class CirculationRequestException extends RuntimeException {

  public CirculationRequestException(String errorMsg) {
    super(errorMsg);
  }

}
