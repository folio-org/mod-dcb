package org.folio.dcb.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.mapper.TransactionAuditMapper;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.TransactionsAuditService;
import org.folio.dcb.service.TransactionsService;
import org.folio.spring.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;

@Service
@RequiredArgsConstructor
@Log4j2
public class TransactionsServiceImpl implements TransactionsService {

  @Qualifier("lendingLibraryService")
  private final LibraryService lendingLibraryService;
  private final TransactionRepository transactionRepository;
  private final TransactionsAuditService transactionsAuditService;
  private final TransactionMapper transactionMapper;
  private final TransactionAuditMapper transactionAuditMapper;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  @Transactional
  public TransactionStatusResponse createCirculationRequest(String dcbTransactionId, DcbTransaction dcbTransaction) {
    log.debug("createCirculationRequest:: creating new transaction request for role {} ", dcbTransaction.getRole());
    TransactionEntity transaction = transactionMapper.mapToEntity(dcbTransactionId, dcbTransaction);
    TransactionAuditEntity transactionAuditEntity = transactionAuditMapper.mapToEntity(transaction);

    if (dcbTransaction.getRole() == LENDER) {
      TransactionStatusResponse transactionStatusResponse =  lendingLibraryService.createTransaction(dcbTransactionId, dcbTransaction);
      createTransactionAuditRecord(transactionAuditEntity, dcbTransactionId, null);
      return transactionStatusResponse;
    } else {
      throw new IllegalArgumentException("Other roles are not implemented");
    }
  }

  @Override
  @Transactional
  public TransactionStatusResponse updateTransactionStatus(String dcbTransactionId, TransactionStatus transactionStatus) {
    return transactionRepository.findById(dcbTransactionId).map(dcbTransaction -> {
      if (dcbTransaction.getStatus() == transactionStatus.getStatus()) {
        throw new IllegalArgumentException(String.format(
          "Current transaction status equal to new transaction status: dcbTransactionId: %s, status: %s", dcbTransactionId, transactionStatus.getStatus()
        ));
      }

      TransactionAuditEntity transactionAuditEntity = transactionAuditMapper.mapToEntity(dcbTransaction);
      if (dcbTransaction.getRole() == LENDER) {
        lendingLibraryService.updateTransactionStatus(dcbTransaction, transactionStatus);
      } else {
        throw new IllegalArgumentException("Other roles are not implemented");
      }
      createTransactionAuditRecord(transactionAuditEntity, dcbTransactionId, null);

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

  private TransactionEntity getTransactionEntityOrThrow(String dcbTransactionId) {
    return transactionRepository.findById(dcbTransactionId)
      .orElseThrow(() -> new NotFoundException(String.format("DCB Transaction was not found by id= %s ", dcbTransactionId)));
  }

  public void createTransactionAuditRecord(TransactionAuditEntity transactionAuditEntity, String dcbTransactionId, String beforeStatus) {
    log.debug("createTransactionAuditRecord:: creating new transaction audit record with id {} ", transactionAuditEntity.getId());
    var updatedTransactionEntity = getTransactionEntityOrThrow(dcbTransactionId);
    try {
      transactionAuditEntity.setAfter(objectMapper.writeValueAsString(updatedTransactionEntity));
      transactionAuditEntity.setAction(updatedTransactionEntity.getStatus().getValue());
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
    if (Objects.nonNull(beforeStatus)) {
      if (transactionAuditEntity.getBefore().contains(beforeStatus)
        || updatedTransactionEntity.getStatus() == TransactionStatus.StatusEnum.CLOSED) {
        transactionsAuditService.createTransactionAuditRecord(transactionAuditEntity);
      }
    } else {
      transactionsAuditService.createTransactionAuditRecord(transactionAuditEntity);
    }
  }
}
