package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.RequestService;
import org.folio.dcb.service.UserService;
import org.springframework.stereotype.Service;

@Service("borrowingLibraryServiceImpl")
@RequiredArgsConstructor
@Log4j2
public class BorrowingLibraryServiceImpl implements LibraryService {
  private static final String TEMP_VALUE_MATERIAL_TYPE_NAME_BOOK = "book";

  private final UserService userService;
  private final RequestService requestService;
  private final CirculationItemService circulationItemService;

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
  }
}
