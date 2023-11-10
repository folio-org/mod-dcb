package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.ServicePointService;
import org.folio.dcb.service.TransactionsService;
import org.folio.spring.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;

@Service
@RequiredArgsConstructor
@Log4j2
public class TransactionsServiceImpl implements TransactionsService {

  @Qualifier("lendingLibraryService")
  private final LibraryService lendingLibraryService;
  @Qualifier("borrowingPickupLibraryService")
  private final LibraryService borrowingPickupLibraryService;
  @Qualifier("pickupLibraryService")
  private final LibraryService pickupLibraryService;
  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;
  private final ServicePointService servicePointService;

  @Override
  public TransactionStatusResponse createCirculationRequest(String dcbTransactionId, DcbTransaction dcbTransaction) {
    log.debug("createCirculationRequest:: creating new transaction request for role {} ", dcbTransaction.getRole());
    checkTransactionExistsAndThrow(dcbTransactionId);
    ServicePointRequest pickupServicePoint = servicePointService.createServicePoint(dcbTransaction.getPickup());

    TransactionStatusResponse circulationStatusResponse =
      switch (dcbTransaction.getRole()) {
        case LENDER -> lendingLibraryService.createCirculation(dcbTransactionId, dcbTransaction, pickupServicePoint.getId());
        case BORROWING_PICKUP -> borrowingPickupLibraryService.createCirculation(dcbTransactionId, dcbTransaction, pickupServicePoint.getId());
        case PICKUP -> pickupLibraryService.createCirculation(dcbTransactionId, dcbTransaction, pickupServicePoint.getId());
        default -> throw new IllegalArgumentException("Other roles are not implemented");
      };

    saveDcbTransaction(dcbTransactionId, dcbTransaction);

    return circulationStatusResponse;
  }

  @Override
  public TransactionStatusResponse updateTransactionStatus(String dcbTransactionId, TransactionStatus transactionStatus) {
    return transactionRepository.findById(dcbTransactionId).map(dcbTransaction -> {
      if (dcbTransaction.getStatus() == transactionStatus.getStatus()) {
        throw new IllegalArgumentException(String.format(
          "Current transaction status equal to new transaction status: dcbTransactionId: %s, status: %s", dcbTransactionId, transactionStatus.getStatus()
        ));
      }
      switch (dcbTransaction.getRole()) {
        case LENDER -> lendingLibraryService.updateTransactionStatus(dcbTransaction, transactionStatus);
        case BORROWING_PICKUP -> borrowingPickupLibraryService.updateTransactionStatus(dcbTransaction, transactionStatus);
        case PICKUP -> pickupLibraryService.updateTransactionStatus(dcbTransaction, transactionStatus);
        default -> throw new IllegalArgumentException("Other roles are not implemented");
      }

      return TransactionStatusResponse.builder()
        .status(TransactionStatusResponse.StatusEnum.fromValue(transactionStatus.getStatus().getValue()))
        .build();
    }).orElseThrow(() -> new IllegalArgumentException(String.format("Transaction with id %s not found", dcbTransactionId)));
  }

  public TransactionStatusResponse getTransactionStatusById(String dcbTransactionId) {
    log.debug("getTransactionStatusById:: id {} ", dcbTransactionId);
    TransactionEntity transactionEntity = getTransactionEntityOrThrow(dcbTransactionId);

    return generateTransactionStatusResponseFromTransactionEntity(transactionEntity);
  }

  private TransactionStatusResponse generateTransactionStatusResponseFromTransactionEntity(TransactionEntity transactionEntity) {
    TransactionStatus.StatusEnum transactionStatus = transactionEntity.getStatus();
    TransactionStatusResponse.StatusEnum transactionStatusResponseStatusEnum = TransactionStatusResponse.StatusEnum.fromValue(transactionStatus.getValue());
    DcbTransaction.RoleEnum transactionRole = transactionEntity.getRole();

    return TransactionStatusResponse.builder()
      .status(transactionStatusResponseStatusEnum)
      .role((TransactionStatusResponse.RoleEnum.fromValue(transactionRole.getValue())))
      .build();
  }

  public TransactionEntity getTransactionEntityOrThrow(String dcbTransactionId) {
    return transactionRepository.findById(dcbTransactionId)
      .orElseThrow(() -> new NotFoundException(String.format("DCB Transaction was not found by id= %s ", dcbTransactionId)));
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
}
