package org.folio.dcb.service;

import org.folio.dcb.domain.entity.TransactionEntity;

public interface CirculationService {
  /**
   * Check in item by barcode
   * @param dcbTransaction dcbTransactionEntity
   */
  void checkInByBarcode(TransactionEntity dcbTransaction);

  void checkInByBarcode(TransactionEntity dcbTransaction, String servicePointId);

  /**
   * Check out item by barcode
   * @param dcbTransaction dcbTransactionEntity
   */
  void checkOutByBarcode(TransactionEntity dcbTransaction);

  /**
   * Cancels a transaction request based on the provided transaction details.
   * <p>
   * If {@code isItemUnavailableCancellation} is {@code true}, the cancellation reason
   * will be updated to indicate item unavailability, and the notification for this
   * cancellation will be suppressed by setting the {@code suppressNotification} flag
   * to {@code true}.
   * </p>
   *
   * @param dcbTransaction the transaction entity representing the request to be canceled
   * @param isItemUnavailableCancellation a flag indicating whether the cancellation is due to item unavailability
   *                                       (true if the item is unavailable, false otherwise)
   */
  void cancelRequest(TransactionEntity dcbTransaction, boolean isItemUnavailableCancellation);
}
