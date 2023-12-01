package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.repository.TransactionAuditRepository;
import org.folio.dcb.service.TransactionAuditService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class TransactionAuditServiceImpl implements TransactionAuditService {
  private static final String ERROR_ACTION = "ERROR";

  private final TransactionMapper transactionMapper;
  private final TransactionAuditRepository transactionAuditRepository;
  @Override
  public void logTheErrorForExistedTransactionAudit(String dcbTransactionId, String errorMsg) {
    log.debug("logTheErrorForExistedTransactionAudit:: dcbTransactionId = {}, err = {}", dcbTransactionId, errorMsg);

    this.logTheErrorForNotExistedTransactionAudit(dcbTransactionId, null, errorMsg);
  }

  @Override
  public void logTheErrorForNotExistedTransactionAudit(String dcbTransactionId, DcbTransaction dcbTransaction, String errorMsg) {
    TransactionAuditEntity transactionAuditEntityExisted = transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(dcbTransactionId).orElse(null);

    if(transactionAuditEntityExisted == null && dcbTransaction != null){
      log.debug("logTheErrorForNotExistedTransactionAudit:: dcbTransactionId = {}, dcbTransaction = {}, err = {}", dcbTransactionId, dcbTransaction, errorMsg);

      TransactionEntity transactionEntity = transactionMapper.mapToEntity(dcbTransactionId, dcbTransaction);
      TransactionAuditEntity trnAEntError = generateTrnAuditEntityByTrnEntityWithError(dcbTransactionId, transactionEntity, errorMsg);

      transactionAuditRepository.save(trnAEntError);
    }

    if(transactionAuditEntityExisted != null){
      TransactionAuditEntity trnAEntError = generateTrnAuditEntityFromTheFoundOneWithError(transactionAuditEntityExisted, errorMsg);
      transactionAuditRepository.save(trnAEntError);
    }
  }

  private TransactionAuditEntity generateTrnAuditEntityFromTheFoundOneWithError(TransactionAuditEntity existed, String errorMsg) {
    TransactionAuditEntity trnAEntError = new TransactionAuditEntity();
    trnAEntError.setId(UUID.randomUUID());
    trnAEntError.setTransactionId(existed.getTransactionId());
    trnAEntError.setAction(ERROR_ACTION);
    trnAEntError.setBefore(existed.getAfter());
    trnAEntError.setAfter(existed.getAfter());
    trnAEntError.setErrorMessage(errorMsg);

    return trnAEntError;
  }

  private TransactionAuditEntity generateTrnAuditEntityByTrnEntityWithError(String dcbTransactionId, TransactionEntity trnE, String errorMsg) {
    String errorMessage = String.format("dcbTransactionId = %s; dcb transaction content = %s; error message = %s.", dcbTransactionId, trnE.toString(), errorMsg);

    TransactionAuditEntity trnAEntError = new TransactionAuditEntity();
    trnAEntError.setId(UUID.randomUUID());
    trnAEntError.setTransactionId(dcbTransactionId);
    trnAEntError.setAction(ERROR_ACTION);
    trnAEntError.setBefore(null);
    trnAEntError.setAfter(null);
    trnAEntError.setErrorMessage(errorMessage);

    return trnAEntError;
  }
}
