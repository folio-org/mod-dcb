package org.folio.dcb.service.impl;

import static java.lang.String.format;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.AWAITING_PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CANCELLED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_IN;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_OUT;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusContext;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.CirculationService;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.ServicePointService;
import org.springframework.stereotype.Service;

@Log4j2
@Service("borrowingLibraryService")
@RequiredArgsConstructor
public class BorrowingLibraryServiceImpl implements LibraryService {

  private final CirculationService circulationService;
  private final TransactionRepository transactionRepository;
  private final BaseLibraryService libraryService;
  private final ServicePointService servicePointService;

  @Override
  public TransactionStatusResponse createCirculation(String dcbTransactionId, DcbTransaction dcbTransaction) {
    ServicePointRequest pickupServicePoint = servicePointService.createServicePointIfNotExists(dcbTransaction);
    dcbTransaction.getPickup().setServicePointId(pickupServicePoint.getId());
    return libraryService.createBorrowingLibraryTransaction(
      dcbTransactionId, dcbTransaction, pickupServicePoint.getId());
  }

  @Override
  public void updateTransactionStatus(TransactionEntity dcbTransaction, TransactionStatus transactionStatus) {
    log.debug("updateTransactionStatus:: Updating transaction {} from {} to {}.",
      dcbTransaction.getId(), dcbTransaction.getStatus(), transactionStatus.getStatus());
    var currStatus = dcbTransaction.getStatus();
    var newStatus = transactionStatus.getStatus();
    String randomServicePointId = UUID.randomUUID().toString();

    if (CREATED == currStatus && OPEN == newStatus) {
      log.info("updateTransactionStatus:: Checking in item for transaction {}.", dcbTransaction.getId());
      circulationService.checkInByBarcode(dcbTransaction, randomServicePointId);
      updateTransactionEntity(dcbTransaction, newStatus);
    } else if (ITEM_CHECKED_OUT == currStatus && ITEM_CHECKED_IN == newStatus) {
      log.info("updateTransactionStatus:: Checking in item for transaction {}.", dcbTransaction.getId());
      Optional.ofNullable(transactionStatus.getContext())
        .map(TransactionStatusContext::getClaimedReturnedResolution)
        .ifPresentOrElse(
          resolution -> circulationService.checkInByBarcode(dcbTransaction, randomServicePointId, resolution),
          () -> circulationService.checkInByBarcode(dcbTransaction, randomServicePointId));

      updateTransactionEntity(dcbTransaction, newStatus);
    } else if (OPEN == currStatus && AWAITING_PICKUP == newStatus) {
      circulationService.checkInByBarcode(dcbTransaction);
      updateTransactionEntity(dcbTransaction, newStatus);
    } else if (AWAITING_PICKUP == currStatus && ITEM_CHECKED_OUT == newStatus) {
      log.info("updateTransactionStatus:: Checking out item for transaction {}.", dcbTransaction.getId());
      circulationService.checkOutByBarcode(dcbTransaction);
      updateTransactionEntity(dcbTransaction, newStatus);
    } else if (ITEM_CHECKED_IN == currStatus && CLOSED == newStatus) {
      log.info("updateTransactionStatus:: transaction {} status transition from {} to {}.",
        dcbTransaction.getId(), ITEM_CHECKED_IN.getValue(), CLOSED.getValue());
      updateTransactionEntity(dcbTransaction, newStatus);
    } else if (CANCELLED == newStatus) {
      log.info("updateTransactionStatus:: Cancelling transaction: {} for Borrower role", dcbTransaction.getId());
      libraryService.cancelTransactionRequest(dcbTransaction);
    } else {
      log.warn("updateTransactionStatus:: status update from {} to {} is not implemented", currStatus, newStatus);
      throw new IllegalArgumentException(
        format("Status update from %s to %s is not implemented", currStatus, newStatus));
    }
  }

  private void updateTransactionEntity(TransactionEntity transaction,
      TransactionStatus.StatusEnum status) {
    log.info("updateTransactionEntity:: updating transaction entity from {} to {}", transaction.getStatus(), status);
    transaction.setStatus(status);
    transactionRepository.save(transaction);
  }
}
