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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


@Service("borrowingPickupLibraryService")
@RequiredArgsConstructor
@Log4j2
public class BorrowingPickupLibraryServiceImpl implements LibraryService {

  private final UserService userService;
  private final RequestService requestService;
  private final CirculationItemService circulationItemService;
  @Qualifier("basicPickupLibraryService")
  private final LibraryService basicPickupLibraryService;

  @Override
  public TransactionStatusResponse createCirculation(String dcbTransactionId, DcbTransaction dcbTransaction, String pickupServicePointId) {
    var itemVirtual = dcbTransaction.getItem();
    var patron = dcbTransaction.getPatron();

    var user = userService.fetchUser(patron); //user is needed, but shouldn't be generated. it should be fetched.
    circulationItemService.checkIfItemExistsAndCreate(itemVirtual, pickupServicePointId);

    requestService.createHoldItemRequest(user, itemVirtual, pickupServicePointId);

    return TransactionStatusResponse.builder()
      .status(TransactionStatusResponse.StatusEnum.CREATED)
      .item(itemVirtual)
      .patron(patron)
      .build();
  }

  @Override
  public void updateStatusByTransactionEntity(TransactionEntity transactionEntity) {
    basicPickupLibraryService.updateStatusByTransactionEntity(transactionEntity);
  }

  @Override
  public void updateTransactionStatus(TransactionEntity dcbTransaction, TransactionStatus transactionStatus) {
    basicPickupLibraryService.updateTransactionStatus(dcbTransaction, transactionStatus);
  }

}
