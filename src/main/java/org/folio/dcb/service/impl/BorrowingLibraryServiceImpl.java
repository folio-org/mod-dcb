package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.CirculationItemRequest;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.RequestService;
import org.folio.dcb.service.UserService;
import org.springframework.stereotype.Service;

import static org.folio.dcb.domain.dto.ItemStatus.NameEnum.AWAITING_PICKUP;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;

@Service("borrowingLibraryService")
@RequiredArgsConstructor
@Log4j2
public class BorrowingLibraryServiceImpl implements LibraryService {
  private static final String TEMP_VALUE_MATERIAL_TYPE_NAME_BOOK = "book";

  private final UserService userService;
  private final RequestService requestService;
  private final CirculationItemService circulationItemService;
  private final TransactionRepository transactionRepository;

  @Override
  public TransactionStatusResponse createCirculation(String dcbTransactionId, DcbTransaction dcbTransaction, String pickupServicePointId) {
    var itemVirtual = dcbTransaction.getItem();
    var patron = dcbTransaction.getPatron();
    checkForMaterialTypeValueAndSetupDefaultIfNeeded(itemVirtual);

    var user = userService.fetchUser(patron); //user is needed, but shouldn't be generated. it should be fetched.
    circulationItemService.checkIfItemExistsAndCreate(itemVirtual, pickupServicePointId);

    requestService.createHoldItemRequest(user, itemVirtual, pickupServicePointId);

    return TransactionStatusResponse.builder()
      .status(TransactionStatusResponse.StatusEnum.CREATED)
      .item(itemVirtual)
      .patron(patron)
      .build();
  }

  private void checkForMaterialTypeValueAndSetupDefaultIfNeeded(DcbItem dcbItem) {
    if(StringUtils.isBlank(dcbItem.getMaterialType())){
      dcbItem.setMaterialType(TEMP_VALUE_MATERIAL_TYPE_NAME_BOOK);
    }
  }

  @Override
  public void updateTransactionStatus(TransactionEntity dcbTransaction, TransactionStatus transactionStatus) {
    log.debug("updateTransactionStatus:: Updating dcbTransaction {} to status {} ", dcbTransaction, transactionStatus);
  }

  @Override
  public void updateStatusByTransactionEntity(TransactionEntity transactionEntity) {
    log.debug("updateTransactionStatus:: Received checkIn event for itemId: {}", transactionEntity.getItemId());
    if(OPEN == transactionEntity.getStatus()) {
      CirculationItemRequest circulationItemRequest = circulationItemService.fetchItemById(transactionEntity.getItemId());
      if(AWAITING_PICKUP == circulationItemRequest.getStatus().getName()) {
        updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.AWAITING_PICKUP);
      } else {
        log.info("updateStatusByTransactionEntity:: Item status is {} . So status of transaction is not updated",
          circulationItemRequest.getStatus().getName());
      }
    }
  }

  private void updateTransactionEntity(TransactionEntity transactionEntity, TransactionStatus.StatusEnum transactionStatusEnum) {
    log.info("updateTransactionEntity:: updating transaction entity from {} to {}", transactionEntity.getStatus(), transactionStatusEnum);
    transactionEntity.setStatus(transactionStatusEnum);
    transactionRepository.save(transactionEntity);
  }
}
