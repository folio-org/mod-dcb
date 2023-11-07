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

import java.util.UUID;

import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.*;

@Service("pickupLibraryService")
@RequiredArgsConstructor
@Log4j2
public class PickupLibraryServiceImpl implements LibraryService {

  private final TransactionRepository transactionRepository;
  private final CirculationService circulationService;

  @Override
  public TransactionStatusResponse createCirculation(String dcbTransactionId, DcbTransaction dcbTransaction, String pickupServicePointId) {
    return null;
  }

  @Override
  public void updateStatusByTransactionEntity(TransactionEntity transactionEntity) {
    log.info("Not implemented yet");
  }

  @Override
  public void updateTransactionStatus(TransactionEntity dcbTransaction, TransactionStatus transactionStatus) {
    log.debug("updateTransactionStatus:: Updating dcbTransaction {} to status {} ", dcbTransaction, transactionStatus);
    var currentStatus = dcbTransaction.getStatus();
    var requestedStatus = transactionStatus.getStatus();
    if (CREATED == currentStatus && OPEN == requestedStatus) {
      log.info("updateTransactionStatus:: Checking in item by barcode: {} ", dcbTransaction.getItemBarcode());
      //Random UUID for servicePointId.
      circulationService.checkInByBarcode(dcbTransaction, UUID.randomUUID().toString());
      updateTransactionEntity(dcbTransaction, requestedStatus);
    } else {
      String errorMessage = String.format("updateTransactionStatus:: status update from %s to %s is not implemented", currentStatus, requestedStatus);
      log.warn(errorMessage);
      throw new IllegalArgumentException(errorMessage);
    }
  }

  private void updateTransactionEntity(TransactionEntity transactionEntity, TransactionStatus.StatusEnum transactionStatusEnum) {
    log.info("updateTransactionEntity:: updating transaction entity from {} to {}", transactionEntity.getStatus(), transactionStatusEnum);
    transactionEntity.setStatus(transactionStatusEnum);
    transactionRepository.save(transactionEntity);
  }
}
