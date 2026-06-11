package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.RequestService;
import org.folio.dcb.service.UserService;
import org.springframework.stereotype.Service;

@Log4j2
@RequiredArgsConstructor
@Service("pickupLibraryService")
public class PickupLibraryServiceImpl implements LibraryService {

  private final UserService userService;
  private final RequestService requestService;
  private final CirculationItemService circulationItemService;
  private final BaseLibraryService baseLibraryService;

  @Override
  public TransactionStatusResponse createCirculation(String dcbTransactionId, DcbTransaction dcbTransaction) {
    var itemVirtual = dcbTransaction.getItem();

    var patron = dcbTransaction.getPatron();
    @SuppressWarnings("VariableDeclarationUsageDistance")
    var user = userService.fetchOrCreateUser(patron);

    baseLibraryService.checkItemExistsInInventoryAndThrow(itemVirtual.getBarcode());
    var pickupServicePointId = dcbTransaction.getPickup().getServicePointId();
    var item = circulationItemService.checkIfItemExistsAndCreate(itemVirtual, pickupServicePointId);

    dcbTransaction.getItem().setId(item.getId());
    baseLibraryService.checkOpenTransactionExistsAndThrow(item.getId());

    var holdRequest = requestService.createHoldItemRequest(user, itemVirtual, pickupServicePointId);
    baseLibraryService.saveDcbTransaction(dcbTransactionId, dcbTransaction, holdRequest.getId());

    return TransactionStatusResponse.builder()
      .status(TransactionStatusResponse.StatusEnum.CREATED)
      .item(itemVirtual)
      .patron(patron)
      .build();
  }

  @Override
  public void updateTransactionStatus(TransactionEntity dcbTransaction, TransactionStatus transactionStatus) {
    baseLibraryService.updateTransactionStatus(dcbTransaction, transactionStatus);
  }
}
