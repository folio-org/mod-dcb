package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.CirculationService;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.RequestService;
import org.folio.dcb.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service("lendingLibraryService")
@RequiredArgsConstructor
@Log4j2
public class LendingLibraryServiceImpl implements LibraryService {

  private final UserService userService;
  private final RequestService requestService;
  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;
  private final CirculationService circulationService;

  @Override
  public TransactionStatusResponse createTransaction(String dcbTransactionId, DcbTransaction dcbTransaction) {
    log.debug("createTransaction:: creating a new transaction with dcbTransactionId {} , dcbTransaction {}",
      dcbTransactionId, dcbTransaction);

    checkTransactionExistsAndThrow(dcbTransactionId);
    var item = dcbTransaction.getItem();
    var patron = dcbTransaction.getPatron();

    var user = userService.fetchOrCreateUser(patron);
    requestService.createPageItemRequest(user, item);
    saveDcbTransaction(dcbTransactionId, dcbTransaction);

    return TransactionStatusResponse.builder()
      .status(TransactionStatusResponse.StatusEnum.CREATED)
      .item(item)
      .patron(patron)
      .build();
  }

  private void checkTransactionExistsAndThrow(String dcbTransactionId) {
    if(transactionRepository.existsById(dcbTransactionId)) {
      throw new ResourceAlreadyExistException(
        String.format("unable to create transaction with id %s as it already exists", dcbTransactionId));
    }
  }

  private void saveDcbTransaction(String dcbTransactionId, DcbTransaction dcbTransaction) {
    TransactionEntity transactionEntity = transactionMapper.mapToEntity(dcbTransactionId, dcbTransaction);
    if (Objects.isNull(transactionEntity)) {
      throw new IllegalArgumentException("Transaction Entity is null");
    }
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionRepository.save(transactionEntity);
  }

  @Override
  public void updateTransactionStatus(TransactionEntity dcbTransaction, TransactionStatus transactionStatus) {
    log.debug("updateTransactionStatus:: Updating dcbTransaction {} to status {} ", dcbTransaction, transactionStatus);
    if (TransactionStatus.StatusEnum.OPEN == (dcbTransaction.getStatus()) && TransactionStatus.StatusEnum.AWAITING_PICKUP == (transactionStatus.getStatus())) {
      log.info("updateTransactionStatus:: Checking in item by barcode: {} ", dcbTransaction.getPatronBarcode());
      circulationService.checkInByBarcode(dcbTransaction);
    } else if (TransactionStatus.StatusEnum.AWAITING_PICKUP == (dcbTransaction.getStatus()) && TransactionStatus.StatusEnum.ITEM_CHECKED_OUT == (transactionStatus.getStatus())) {
      log.info("updateTransactionStatus:: Checking out item by barcode: {} ", dcbTransaction.getPatronBarcode());
      circulationService.checkOutByBarcode(dcbTransaction);
    } else {
      throw new IllegalArgumentException("Other statuses are not implemented yet");
    }

    dcbTransaction.setStatus(transactionStatus.getStatus());
    transactionRepository.save(dcbTransaction);
  }

  @Override
  public void updateStatusByTransactionEntity(TransactionEntity transactionEntity) {
    log.debug("updateTransactionStatus:: Received checkIn event for itemId: {}", transactionEntity.getItemId());
    if (TransactionStatus.StatusEnum.CREATED == (transactionEntity.getStatus())) {
      transactionEntity.setStatus(TransactionStatus.StatusEnum.OPEN);
      transactionRepository.save(transactionEntity);
      log.info("updateTransactionStatus:: Transaction status updated from CREATED to OPEN for itemId: {}", transactionEntity.getItemId());
    }
  }
}
