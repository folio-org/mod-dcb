package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.*;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.CirculationService;
import org.folio.dcb.service.RequestService;
import org.folio.dcb.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

import static org.folio.dcb.domain.dto.ItemStatus.NameEnum.AWAITING_PICKUP;
import static org.folio.dcb.domain.dto.ItemStatus.NameEnum.CHECKED_OUT;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_IN;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_OUT;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;

@Service
@RequiredArgsConstructor
@Log4j2
public class BaseLibraryService {

  private static final String UPDATE_STATUS_BY_TRANSACTION_ENTITY_LOG_MESSAGE_PATTERN = "updateStatusByTransactionEntity:: Updated item status from {} to {}";

  private final TransactionRepository transactionRepository;
  private final CirculationService circulationService;
  private final CirculationItemService circulationItemService;
  private final UserService userService;
  private final RequestService requestService;
  private final TransactionMapper transactionMapper;

  public TransactionStatusResponse createBorrowingLibraryTransaction(String dcbTransactionId, DcbTransaction dcbTransaction, String pickupServicePointId) {
    var itemVirtual = dcbTransaction.getItem();
    var patron = dcbTransaction.getPatron();

    var user = userService.fetchUser(patron); //user is needed, but shouldn't be generated. it should be fetched.
    circulationItemService.checkIfItemExistsAndCreate(itemVirtual, pickupServicePointId);

    requestService.createHoldItemRequest(user, itemVirtual, pickupServicePointId);
    saveDcbTransaction(dcbTransactionId, dcbTransaction);

    return TransactionStatusResponse.builder()
      .status(TransactionStatusResponse.StatusEnum.CREATED)
      .item(itemVirtual)
      .patron(patron)
      .build();
  }
  public void saveDcbTransaction(String dcbTransactionId, DcbTransaction dcbTransaction) {
    TransactionEntity transactionEntity = transactionMapper.mapToEntity(dcbTransactionId, dcbTransaction);
    if (Objects.isNull(transactionEntity)) {
      throw new IllegalArgumentException("Transaction Entity is null");
    }
    transactionEntity.setStatus(CREATED);
    transactionRepository.save(transactionEntity);
  }

  public void updateStatusByTransactionEntity(TransactionEntity transactionEntity) {
    log.debug("updateTransactionStatus:: Received checkIn event for itemId: {}", transactionEntity.getItemId());
    CirculationItemRequest circulationItemRequest = circulationItemService.fetchItemById(transactionEntity.getItemId());
    var circulationItemRequestStatus = circulationItemRequest.getStatus().getName();
    if (OPEN == transactionEntity.getStatus() && AWAITING_PICKUP == circulationItemRequestStatus) {
      log.info(UPDATE_STATUS_BY_TRANSACTION_ENTITY_LOG_MESSAGE_PATTERN,
        transactionEntity.getStatus().getValue(), TransactionStatus.StatusEnum.AWAITING_PICKUP.getValue());
      updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.AWAITING_PICKUP);
    } else if (TransactionStatus.StatusEnum.AWAITING_PICKUP == transactionEntity.getStatus() && CHECKED_OUT == circulationItemRequestStatus) {
      log.info(UPDATE_STATUS_BY_TRANSACTION_ENTITY_LOG_MESSAGE_PATTERN,
        transactionEntity.getStatus().getValue(), ITEM_CHECKED_OUT.getValue());
      updateTransactionEntity(transactionEntity, ITEM_CHECKED_OUT);
    } else if(ITEM_CHECKED_OUT == transactionEntity.getStatus()){
      log.info(UPDATE_STATUS_BY_TRANSACTION_ENTITY_LOG_MESSAGE_PATTERN,
        transactionEntity.getStatus().getValue(), TransactionStatus.StatusEnum.ITEM_CHECKED_IN.getValue());
      updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    } else {
      log.info("updateStatusByTransactionEntity:: Item status is {}. So status of transaction is not updated",
        circulationItemRequestStatus);
    }

  }

  public void updateTransactionStatus(TransactionEntity dcbTransaction, TransactionStatus transactionStatus) {
    log.debug("updateTransactionStatus:: Updating dcbTransaction {} to status {} ", dcbTransaction, transactionStatus);
    var currentStatus = dcbTransaction.getStatus();
    var requestedStatus = transactionStatus.getStatus();
    if (CREATED == currentStatus && OPEN == requestedStatus) {
      log.info("updateTransactionStatus:: Checking in item by barcode: {} ", dcbTransaction.getItemBarcode());
      //Random UUID for servicePointId.
      circulationService.checkInByBarcode(dcbTransaction, UUID.randomUUID().toString());
      updateTransactionEntity(dcbTransaction, requestedStatus);
    } else if (ITEM_CHECKED_IN == currentStatus && CLOSED == requestedStatus) {
      log.info("updateTransactionStatus:: transaction status transition from {} to {} for the item with barcode {} ",
        ITEM_CHECKED_IN.getValue(), CLOSED.getValue(), dcbTransaction.getItemBarcode());
      updateTransactionEntity(dcbTransaction, requestedStatus);
    } else {
      String errorMessage = String.format("updateTransactionStatus:: status update from %s to %s is not implemented", currentStatus, requestedStatus);
      log.warn(errorMessage);
      throw new IllegalArgumentException(errorMessage);
    }
  }

  private void updateTransactionEntity(TransactionEntity transactionEntity, TransactionStatus.StatusEnum transactionStatusEnum) {
    log.debug("updateTransactionEntity:: updating transaction entity from {} to {}", transactionEntity.getStatus(), transactionStatusEnum);
    transactionEntity.setStatus(transactionStatusEnum);
    transactionRepository.save(transactionEntity);
  }
}
