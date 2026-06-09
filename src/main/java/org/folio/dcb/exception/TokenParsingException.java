package org.folio.dcb.exception;

import lombok.Getter;

/**
 * Exception thrown when token parsing fails.
 *
 * <p>This exception is thrown when an error occurs during token parsing operations, such as JWT
 * token validation, extraction, or deserialization. The exception message contains details about the parsing failure.
 */
@Getter
public class TokenParsingException extends RuntimeException {

  /**
   * Constructs a new TokenParsingException with the specified message.
   *
   * @param message the detail message describing the token parsing error
   */
  public TokenParsingException(String message) {
    super(message);
  }
}
