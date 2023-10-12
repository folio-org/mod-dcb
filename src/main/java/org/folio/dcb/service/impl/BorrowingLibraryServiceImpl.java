package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.RequestService;
import org.folio.dcb.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;

@Service("borrowingLibraryServiceImpl")
@RequiredArgsConstructor
@Log4j2
public class BorrowingLibraryServiceImpl implements LibraryService {

  private final UserService userService;
  private final RequestService requestService;
  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;
  private final CirculationItemService circulationItemService;

  @Override
  public TransactionStatusResponse createTransaction(String dcbTransactionId, DcbTransaction dcbTransaction) {
    log.debug("createTransaction:: creating a new transaction with dcbTransactionId {} , dcbTransaction {}",
      dcbTransactionId, dcbTransaction);

    checkTransactionExistsAndThrow(dcbTransactionId);
    var itemVirtual = dcbTransaction.getItem();
    var patron = dcbTransaction.getPatron();

    var user = userService.fetchUser(patron); //user is needed, but shouldn't be generated. it should be fetched.
    var item = circulationItemService.fetchOrCreateItem(itemVirtual);

    requestService.createHoldItemRequest(user, item);
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
    transactionEntity.setStatus(CREATED);
    transactionRepository.save(transactionEntity);
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
