package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.RequestService;
import org.folio.dcb.service.UserService;
import org.springframework.stereotype.Service;

@Service("pickupLibraryService")
@RequiredArgsConstructor
@Log4j2
public class PickupLibraryServiceImpl implements LibraryService {

  private final UserService userService;
  private final RequestService requestService;
  private final CirculationItemService circulationItemService;
  private final BaseLibraryService baseLibraryService;

  @Override
  public TransactionStatusResponse createCirculation(String dcbTransactionId, DcbTransaction dcbTransaction, String pickupServicePointId) {
    var itemVirtual = dcbTransaction.getItem();
    var patron = dcbTransaction.getPatron();

    var user = userService.fetchOrCreateUser(patron);
    baseLibraryService.checkUserTypeAndThrowIfMismatch(user.getType());
    baseLibraryService.checkItemExistsInInventoryAndThrow(itemVirtual.getBarcode());
    circulationItemService.checkIfItemExistsAndCreate(itemVirtual, pickupServicePointId);

    CirculationRequest holdRequest = requestService.createHoldItemRequest(user, itemVirtual, pickupServicePointId);
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
