package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.dto.Role;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.TransactionsService;
import org.folio.spring.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class TransactionsServiceImpl implements TransactionsService {

  @Qualifier("lendingLibraryService")
  private final LibraryService lendingLibraryService;
  private final TransactionRepository transactionRepository;

  @Override
  public TransactionStatusResponse createCirculationRequest(String dcbTransactionId, DcbTransaction dcbTransaction) {
    log.debug("createCirculationRequest:: creating new transaction request for role {} ", dcbTransaction.getRole());
    return switch (dcbTransaction.getRole().getTransactionRole()) {
      case LENDER -> lendingLibraryService.createTransaction(dcbTransactionId, dcbTransaction);
      default -> throw new IllegalArgumentException("Other roles are not implemented");
    };
  }

  public TransactionStatusResponse getTransactionStatusById(String dcbTransactionId) {
    log.debug("getTransactionStatusById:: id {} ", dcbTransactionId);
    TransactionEntity transactionEntity = getTransactionEntityOrThrow(dcbTransactionId);

    return generateTransactionStatusResponseFromTransactionEntity(transactionEntity);
  }

  private TransactionStatusResponse generateTransactionStatusResponseFromTransactionEntity(TransactionEntity transactionEntity) {
    TransactionStatus.StatusEnum transactionStatus = transactionEntity.getStatus();
    Role.TransactionRoleEnum transactionRole = transactionEntity.getRole();
    TransactionStatusResponse.StatusEnum transactionStatusResponseStatusEnum = TransactionStatusResponse.StatusEnum.fromValue(transactionStatus.getValue());
    TransactionStatusResponse.TransactionRoleEnum transactionStatusResponseRoleEnum = TransactionStatusResponse.TransactionRoleEnum.fromValue(transactionRole.getValue());

    return TransactionStatusResponse.builder()
      .status(transactionStatusResponseStatusEnum)
      .transactionRole(transactionStatusResponseRoleEnum)
      .build();
  }

  private TransactionEntity getTransactionEntityOrThrow(String dcbTransactionId) {
    return transactionRepository.findById(dcbTransactionId)
      .orElseThrow(() -> new NotFoundException(String.format("DCB Transaction was not found by id= %s ", dcbTransactionId)));
  }

  }
