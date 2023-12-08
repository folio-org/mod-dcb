package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.CirculationService;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.ServicePointService;
import org.springframework.stereotype.Service;
import java.util.UUID;

import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.AWAITING_PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_IN;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_OUT;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CANCELLED;

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
    ServicePointRequest pickupServicePoint = servicePointService.createServicePoint(dcbTransaction.getPickup());
    return libraryService.createBorrowingLibraryTransaction(dcbTransactionId, dcbTransaction, pickupServicePoint.getId());
  }

  @Override
  public void updateStatusByTransactionEntity(TransactionEntity transactionEntity) {
  }

  @Override
  public void updateTransactionStatus(TransactionEntity dcbTransaction, TransactionStatus transactionStatus) {
    log.debug("updateTransactionStatus:: Updating dcbTransaction {} to status {} ", dcbTransaction, transactionStatus);
    var currentStatus = dcbTransaction.getStatus();
    var requestedStatus = transactionStatus.getStatus();

    if ((CREATED == currentStatus && OPEN == requestedStatus) || (ITEM_CHECKED_OUT == currentStatus && ITEM_CHECKED_IN == requestedStatus)) {
      log.info("updateTransactionStatus:: Checking in item by barcode: {} ", dcbTransaction.getItemBarcode());
      //Random UUID for servicePointId.
      circulationService.checkInByBarcode(dcbTransaction, UUID.randomUUID().toString());
      updateTransactionEntity(dcbTransaction, requestedStatus);
    } else if(OPEN == currentStatus && AWAITING_PICKUP == requestedStatus) {
      circulationService.checkInByBarcode(dcbTransaction);
      updateTransactionEntity(dcbTransaction, requestedStatus);
    } else if (AWAITING_PICKUP == currentStatus && ITEM_CHECKED_OUT == requestedStatus) {
      log.info("updateTransactionStatus:: Checking out item by barcode: {} ", dcbTransaction.getPatronBarcode());
      circulationService.checkOutByBarcode(dcbTransaction);
      updateTransactionEntity(dcbTransaction, requestedStatus);
    } else if (ITEM_CHECKED_IN == currentStatus && CLOSED == requestedStatus) {
      log.info("updateTransactionStatus:: transaction status transition from {} to {} for the item with barcode {} ",
        ITEM_CHECKED_IN.getValue(), CLOSED.getValue(), dcbTransaction.getItemBarcode());
      updateTransactionEntity(dcbTransaction, requestedStatus);
    } else if(CANCELLED == requestedStatus) {
      log.info("updateTransactionStatus:: Cancelling transaction with id: {} for Borrower role", dcbTransaction.getId());
      libraryService.cancelTransactionRequest(dcbTransaction);
    } else {
      String error = String.format("updateTransactionStatus:: status update from %s to %s is not implemented", currentStatus, requestedStatus);
      log.warn(error);
      throw new IllegalArgumentException(error);
    }
  }

  private void updateTransactionEntity(TransactionEntity transactionEntity, TransactionStatus.StatusEnum transactionStatusEnum) {
    log.info("updateTransactionEntity:: updating transaction entity from {} to {}", transactionEntity.getStatus(), transactionStatusEnum);
    transactionEntity.setStatus(transactionStatusEnum);
    transactionRepository.save(transactionEntity);
  }

}
