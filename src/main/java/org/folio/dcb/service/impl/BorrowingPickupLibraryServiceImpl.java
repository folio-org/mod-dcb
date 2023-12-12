package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.service.LibraryService;
import org.springframework.stereotype.Service;


@Service("borrowingPickupLibraryService")
@RequiredArgsConstructor
@Log4j2
public class BorrowingPickupLibraryServiceImpl implements LibraryService {

  private final BaseLibraryService baseLibraryService;

  @Override
  public TransactionStatusResponse createCirculation(String dcbTransactionId, DcbTransaction dcbTransaction) {
    return baseLibraryService.createBorrowingLibraryTransaction(dcbTransactionId, dcbTransaction, dcbTransaction.getPickup().getServicePointId());
  }

  @Override
  public void updateStatusByTransactionEntity(TransactionEntity transactionEntity) {
    baseLibraryService.updateStatusByTransactionEntity(transactionEntity);
  }

  @Override
  public void updateTransactionStatus(TransactionEntity dcbTransaction, TransactionStatus transactionStatus) {
    baseLibraryService.updateTransactionStatus(dcbTransaction, transactionStatus);
  }

}
