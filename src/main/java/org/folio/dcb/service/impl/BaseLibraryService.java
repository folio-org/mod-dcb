package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.DcbUpdateItem;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.exception.CirculationRequestException;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.CirculationService;
import org.folio.dcb.service.ItemService;
import org.folio.dcb.service.RequestService;
import org.folio.dcb.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CANCELLED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_IN;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;
import static org.folio.dcb.utils.DCBConstants.DCB_TYPE;
import static org.folio.dcb.utils.DCBConstants.SHADOW_TYPE;

@Service
@RequiredArgsConstructor
@Log4j2
public class BaseLibraryService {

  private final TransactionRepository transactionRepository;
  private final CirculationService circulationService;
  private final CirculationItemService circulationItemService;
  private final UserService userService;
  private final RequestService requestService;
  private final TransactionMapper transactionMapper;
  private final ItemService itemService;

  public TransactionStatusResponse createBorrowingLibraryTransaction(String dcbTransactionId, DcbTransaction dcbTransaction, String pickupServicePointId) {
    // Item is real when if self`s borrowing is true, otherwise virtual.
    var itemVirtualOrReal = dcbTransaction.getItem();
    var patron = dcbTransaction.getPatron();
    Boolean selfBorrowing = Optional.ofNullable(dcbTransaction.getSelfBorrowing()).orElse(false);

    var user = userService.fetchUser(patron); //user is needed, but shouldn't be generated. it should be fetched.
    if (Objects.equals(user.getType(), DCB_TYPE)) {
      throw new IllegalArgumentException(String.format("User with type %s is retrieved. so unable to create " +
              "transaction", user.getType()));
    }

    if (BooleanUtils.isFalse(selfBorrowing)) {
      // check existence of virtual item only if selfBorrowing is false, item will be real if selfBorrowing is true.
      checkItemExistsInInventoryAndThrow(itemVirtualOrReal.getBarcode());
      CirculationItem item = circulationItemService.checkIfItemExistsAndCreate(itemVirtualOrReal, pickupServicePointId);
      dcbTransaction.getItem().setId(item.getId());
    }
    checkOpenTransactionExistsAndThrow(dcbTransaction.getItem().getId());

    if (BooleanUtils.isFalse(selfBorrowing)) {
      CirculationRequest holdRequest = requestService.createHoldItemRequest(user, itemVirtualOrReal,
              pickupServicePointId);
      saveDcbTransaction(dcbTransactionId, dcbTransaction, holdRequest.getId());
    } else {
      CirculationRequest pageRequest = requestService.createRequestBasedOnItemStatus(user, itemVirtualOrReal,
              pickupServicePointId);
      saveDcbTransaction(dcbTransactionId, dcbTransaction, pageRequest.getId());
    }

    return TransactionStatusResponse.builder()
      .status(TransactionStatusResponse.StatusEnum.CREATED)
      .item(itemVirtualOrReal)
      .patron(patron)
      .build();
  }

  public void saveDcbTransaction(String dcbTransactionId, DcbTransaction dcbTransaction, String requestId) {
    TransactionEntity transactionEntity = transactionMapper.mapToEntity(dcbTransactionId, dcbTransaction);
    if (Objects.isNull(transactionEntity)) {
      throw new IllegalArgumentException("Transaction Entity is null");
    }
    transactionEntity.setRequestId(UUID.fromString(requestId));
    transactionEntity.setStatus(CREATED);
    transactionRepository.save(transactionEntity);
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
    } else if(CANCELLED == requestedStatus) {
      log.info("updateTransactionStatus:: Cancelling transaction with id: {} for Borrower/Pickup role", dcbTransaction.getId());
      cancelTransactionRequest(dcbTransaction);
    } else {
      String errorMessage = String.format("updateTransactionStatus:: status update from %s to %s is not implemented", currentStatus, requestedStatus);
      log.warn(errorMessage);
      throw new IllegalArgumentException(errorMessage);
    }
  }

  public void cancelTransactionRequest(TransactionEntity transactionEntity){
    try {
      circulationService.cancelRequest(transactionEntity, false);
    } catch (CirculationRequestException e) {
      log.error("cancelTransactionRequest:: error during updating circulation request " +
        "to cancel status", e);
      updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.ERROR);
    }
  }

  public void cancelTransactionEntity(TransactionEntity transactionEntity) {
    log.info("cancelTransactionEntity:: Transaction cancelled for itemId: {}", transactionEntity.getItemId());
    updateTransactionEntity(transactionEntity, CANCELLED);
  }

  public void checkItemExistsInInventoryAndThrow(String itemBarcode) {
    if(itemService.fetchItemByBarcode(itemBarcode).getTotalRecords() != 0)
      throw new ResourceAlreadyExistException(String.format("Unable to create item with barcode %s as it exists in inventory ", itemBarcode));
  }

  public void checkOpenTransactionExistsAndThrow(String itemId) {
    if(!transactionRepository.findTransactionsByItemIdAndStatusNotInClosed(UUID.fromString(itemId)).isEmpty()){
      throw new ResourceAlreadyExistException(String.format("Item with id %s already has an open DCB transaction ", itemId));
    }
  }

  public void updateTransactionEntity(TransactionEntity transactionEntity, TransactionStatus.StatusEnum transactionStatusEnum) {
    log.debug("updateTransactionEntity:: updating transaction entity from {} to {}", transactionEntity.getStatus(), transactionStatusEnum);
    transactionEntity.setStatus(transactionStatusEnum);
    transactionRepository.save(transactionEntity);
  }

  public void updateTransactionDetails(TransactionEntity transactionEntity, DcbUpdateItem dcbUpdateItem) {
    DcbPatron dcbPatron = transactionMapper.mapTransactionEntityToDcbPatron(transactionEntity);
    DcbItem dcbItem = transactionMapper.convertTransactionUpdateItemToDcbItem(dcbUpdateItem, transactionEntity);
    checkItemExistsInInventoryAndThrow(dcbItem.getBarcode());
    CirculationItem item = circulationItemService.checkIfItemExistsAndCreate(dcbItem, transactionEntity.getServicePointId());
    dcbItem.setId(item.getId());
    checkOpenTransactionExistsAndThrow(item.getId());
    circulationService.cancelRequest(transactionEntity, true);
    CirculationRequest holdRequest = requestService.createHoldItemRequest(userService.fetchUser(dcbPatron), dcbItem,
      transactionEntity.getServicePointId());
    updateItemDetailsAndSaveEntity(transactionEntity, item, dcbItem.getMaterialType(), holdRequest.getId());
  }

  private void updateItemDetailsAndSaveEntity(TransactionEntity transactionEntity, CirculationItem item,
                                              String materialType, String requestId) {
    transactionEntity.setItemId(item.getId());
    transactionEntity.setRequestId(UUID.fromString(requestId));
    transactionEntity.setItemBarcode(item.getBarcode());
    transactionEntity.setLendingLibraryCode(item.getLendingLibraryCode());
    transactionEntity.setMaterialType(materialType);
    transactionEntity.setStatus(CREATED);
    transactionRepository.save(transactionEntity);
  }
}
