package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.dto.TransactionStatusResponseCollection;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.exception.StatusException;
import org.folio.dcb.repository.TransactionAuditRepository;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.StatusProcessorService;
import org.folio.dcb.service.TransactionsService;
import org.folio.spring.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;

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
  @Qualifier("borrowingLibraryService")
  private final LibraryService borrowingLibraryService;
  private final TransactionRepository transactionRepository;
  private final StatusProcessorService statusProcessorService;
  private final TransactionMapper transactionMapper;
  private final TransactionAuditRepository transactionAuditRepository;

  @Override
  public TransactionStatusResponse createCirculationRequest(String dcbTransactionId, DcbTransaction dcbTransaction) {
    log.debug("createCirculationRequest:: creating new transaction request for role {} ", dcbTransaction.getRole());
    checkTransactionExistsAndThrow(dcbTransactionId);

    return switch (dcbTransaction.getRole()) {
      case LENDER -> lendingLibraryService.createCirculation(dcbTransactionId, dcbTransaction);
      case BORROWING_PICKUP -> borrowingPickupLibraryService.createCirculation(dcbTransactionId, dcbTransaction);
      case PICKUP -> pickupLibraryService.createCirculation(dcbTransactionId, dcbTransaction);
      case BORROWER -> borrowingLibraryService.createCirculation(dcbTransactionId, dcbTransaction);
    };
  }

  @Override
  public TransactionStatusResponse updateTransactionStatus(String dcbTransactionId, TransactionStatus transactionStatus) {
    return transactionRepository.findById(dcbTransactionId).map(dcbTransaction -> {
      if (dcbTransaction.getStatus() == transactionStatus.getStatus()) {
        throw new StatusException(String.format(
          "Current transaction status equal to new transaction status: dcbTransactionId: %s, status: %s", dcbTransactionId, transactionStatus.getStatus()
        ));
      } else if (transactionStatus.getStatus() == TransactionStatus.StatusEnum.CANCELLED
        && (dcbTransaction.getStatus() == TransactionStatus.StatusEnum.ITEM_CHECKED_IN ||
        dcbTransaction.getStatus() == TransactionStatus.StatusEnum.ITEM_CHECKED_OUT) ||
        dcbTransaction.getStatus() == TransactionStatus.StatusEnum.CLOSED) {
        throw new StatusException(String.format(
          "Cannot cancel transaction dcbTransactionId: %s. Transaction already in status: %s: ", dcbTransactionId, dcbTransaction.getStatus()
        ));
      }
      switch (dcbTransaction.getRole()) {
        case LENDER -> statusProcessorService.lendingChainProcessor(dcbTransaction.getStatus(), transactionStatus.getStatus())
          .forEach(statusEnum -> lendingLibraryService.updateTransactionStatus(dcbTransaction, TransactionStatus.builder().status(statusEnum).build()));
        case BORROWING_PICKUP -> borrowingPickupLibraryService.updateTransactionStatus(dcbTransaction, transactionStatus);
        case PICKUP -> pickupLibraryService.updateTransactionStatus(dcbTransaction, transactionStatus);
        case BORROWER -> statusProcessorService.borrowingChainProcessor(dcbTransaction.getStatus(), transactionStatus.getStatus())
          .forEach(statusEnum -> borrowingLibraryService.updateTransactionStatus(dcbTransaction, TransactionStatus.builder().status(statusEnum).build()));
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

  @Override
  public TransactionStatusResponseCollection getTransactionStatusList(OffsetDateTime fromDate, OffsetDateTime toDate, Integer pageNumber, Integer pageSize) {
    log.info("getTransactionStatusList:: fromDate {}, toDate {}, pageNumber {}, pageSize {}",
      fromDate, toDate, pageNumber, pageSize);
    var pageable = PageRequest.of(pageNumber, pageSize, Sort.by("updated_Date"));
    var transactionAuditEntityPage= transactionAuditRepository.findTransactionsByDateRange(fromDate, toDate, pageable);
    var transactionStatusResponseList= transactionMapper.mapToDto(transactionAuditEntityPage);
    return TransactionStatusResponseCollection
      .builder()
      .transactions(transactionStatusResponseList)
      .totalRecords((int)transactionAuditEntityPage.getTotalElements())
      .build();
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
}
