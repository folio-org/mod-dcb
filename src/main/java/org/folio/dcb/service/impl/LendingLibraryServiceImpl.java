package org.folio.dcb.service.impl;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.dcb.client.feign.InventoryItemStorageClient;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.InventoryItem;
import org.folio.dcb.domain.dto.ItemStatus;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.CirculationService;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.RequestService;
import org.folio.dcb.service.ServicePointService;
import org.folio.dcb.service.UserService;
import org.springframework.stereotype.Service;

import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.AWAITING_PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.EXPIRED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_IN;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_OUT;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CANCELLED;
import static org.folio.dcb.utils.CqlQuery.exactMatchById;

@Service("lendingLibraryService")
@RequiredArgsConstructor
@Log4j2
public class LendingLibraryServiceImpl implements LibraryService {

  private final UserService userService;
  private final RequestService requestService;
  private final TransactionRepository transactionRepository;
  private final CirculationService circulationService;
  private final BaseLibraryService baseLibraryService;
  private final ServicePointService servicePointService;
  private final InventoryItemStorageClient itemStorageClient;

  @Override
  public TransactionStatusResponse createCirculation(String dcbTransactionId, DcbTransaction dcbTransaction) {
    log.debug("createTransaction:: creating a new transaction with dcbTransactionId {} , dcbTransaction {}",
      dcbTransactionId, dcbTransaction);

    var item = dcbTransaction.getItem();
    var patron = dcbTransaction.getPatron();

    var user = userService.fetchOrCreateUser(patron);
    ServicePointRequest pickupServicePoint = servicePointService.createServicePointIfNotExists(dcbTransaction.getPickup());
    dcbTransaction.getPickup().setServicePointId(pickupServicePoint.getId());
    CirculationRequest pageRequest = requestService.createRequestBasedOnItemStatus(user, item, pickupServicePoint.getId());
    baseLibraryService.saveDcbTransaction(dcbTransactionId, dcbTransaction, pageRequest.getId());

    return TransactionStatusResponse.builder()
      .status(TransactionStatusResponse.StatusEnum.CREATED)
      .item(item)
      .patron(patron)
      .build();
  }

  @Override
  public void updateTransactionStatus(TransactionEntity dcbTransaction, TransactionStatus transactionStatus) {
    log.debug("updateTransactionStatus:: Updating dcbTransaction {} to status {} ", dcbTransaction, transactionStatus);
    var currentStatus = dcbTransaction.getStatus();
    var requestedStatus = transactionStatus.getStatus();
    if (CREATED == currentStatus && OPEN == requestedStatus) {
      updateTransactionEntity(dcbTransaction, requestedStatus);
    } else if (OPEN == currentStatus && AWAITING_PICKUP == requestedStatus) {
      log.info("updateTransactionStatus:: Checking in item by barcode: {} ", dcbTransaction.getItemBarcode());
      circulationService.checkInByBarcode(dcbTransaction);
      updateTransactionEntity(dcbTransaction, requestedStatus);
    } else if (AWAITING_PICKUP == currentStatus && ITEM_CHECKED_OUT == requestedStatus) {
      log.info("updateTransactionStatus:: Checking out item by barcode: {} ", dcbTransaction.getPatronBarcode());
      circulationService.checkOutByBarcode(dcbTransaction);
      updateTransactionEntity(dcbTransaction, requestedStatus);
    } else if (ITEM_CHECKED_OUT == currentStatus && ITEM_CHECKED_IN == requestedStatus) {
      updateTransactionEntity(dcbTransaction, requestedStatus);
    } else if(EXPIRED == currentStatus && CLOSED == requestedStatus) {
      closeTransactionEntityIfItemIsAvailable(dcbTransaction);
    } else if(CANCELLED == requestedStatus) {
      log.info("updateTransactionStatus:: Cancelling transaction with id: {} for Lender role", dcbTransaction.getId());
      baseLibraryService.cancelTransactionRequest(dcbTransaction);
    } else {
      String errorMessage = String.format("updateTransactionStatus:: status update from %s to %s is not implemented",
        currentStatus, requestedStatus);
      log.warn(errorMessage);
      throw new IllegalArgumentException(errorMessage);
    }
  }

  private void updateTransactionEntity (TransactionEntity transactionEntity, TransactionStatus.StatusEnum transactionStatusEnum) {
    log.info("updateTransactionEntity:: updating transaction entity from {} to {}", transactionEntity.getStatus(), transactionStatusEnum);
    transactionEntity.setStatus(transactionStatusEnum);
    transactionRepository.save(transactionEntity);
  }

  private void closeTransactionEntityIfItemIsAvailable(TransactionEntity transactionEntity) {
    var itemId = transactionEntity.getItemId();
    var inventoryItems = itemStorageClient.fetchItemByQuery(exactMatchById(itemId));
    if (isItemNotAvailable(inventoryItems.getResult())) {
      log.debug("closeTransactionEntityIfItemIsAvailable:: item is not found or available,  {}", itemId);
      return;
    }

    log.debug("closeTransactionEntityIfItemIsAvailable:: closing expired transaction: {}", transactionEntity.getId());
    updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.CLOSED);
  }

  private static boolean isItemNotAvailable(List<InventoryItem> items) {
    return Optional.ofNullable(items)
      .filter(CollectionUtils::isNotEmpty)
      .map(List::getFirst)
      .map(InventoryItem::getStatus)
      .map(ItemStatus::getName)
      .map(statusName -> ItemStatus.NameEnum.AVAILABLE != statusName)
      .orElse(true);
  }
}
