package org.folio.dcb.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DcbHubLocationException extends RuntimeException {

  private final HttpStatus httpStatus;

  public DcbHubLocationException(String message, HttpStatus httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
  }

  public DcbHubLocationException(String message, HttpStatus httpStatus, Throwable cause) {
    super(message, cause);
    this.httpStatus = httpStatus;
  }
}
