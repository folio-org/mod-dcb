package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.CirculationService;
import org.folio.dcb.service.LibraryService;
import org.springframework.stereotype.Service;

import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.AWAITING_PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CANCELLED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;

@Log4j2
@Service("borrowingLibraryService")
@RequiredArgsConstructor
public class BorrowingLibraryServiceImpl implements LibraryService {

  private final CirculationService circulationService;
  private final TransactionRepository transactionRepository;
  private final BaseLibraryService libraryService;

  @Override
  public TransactionStatusResponse createCirculation(String dcbTransactionId, DcbTransaction dcbTransaction, String pickupServicePointId) {
    return libraryService.createBorrowingLibraryTransaction(dcbTransactionId, dcbTransaction, pickupServicePointId);
  }

  @Override
  public void updateStatusByTransactionEntity(TransactionEntity transactionEntity) {
  }

  @Override
  public void updateTransactionStatus(TransactionEntity dcbTransaction, TransactionStatus transactionStatus) {
    log.debug("updateTransactionStatus:: Updating dcbTransaction {} to status {} ", dcbTransaction, transactionStatus);
    var currentStatus = dcbTransaction.getStatus();
    var requestedStatus = transactionStatus.getStatus();
    if(OPEN == currentStatus && AWAITING_PICKUP == requestedStatus) {
      circulationService.checkInByBarcode(dcbTransaction);
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
