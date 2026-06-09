package org.folio.dcb.exception;

/**
 * Exception thrown when an error occurs during circulation request operations.
 *
 * <p>This exception is thrown when operations related to circulation requests (such as creation,
 * validation, or processing) fail or encounter an error. Circulation requests typically involve loan and hold
 * operations within the system.
 */
public class CirculationRequestException extends RuntimeException {

  /**
   * Constructs a new CirculationRequestException with the specified error message.
   *
   * @param errorMsg the detail message describing the circulation request error
   */
  public CirculationRequestException(String errorMsg) {
    super(errorMsg);
  }
}
