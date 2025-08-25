package org.folio.dcb.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class DcbHubLocationException extends RuntimeException{

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
