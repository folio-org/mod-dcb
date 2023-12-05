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
  private static final String DUPLICATE_ERROR_ACTION = "DUPLICATE_ERROR";
  private static final String DUPLICATE_ERROR_TRANSACTION_ID = "-1";

  private final TransactionMapper transactionMapper;
  private final TransactionAuditRepository transactionAuditRepository;
  @Override
  public void logErrorIfTransactionAuditExists(String dcbTransactionId, String errorMsg) {
    log.debug("logTheErrorForExistedTransactionAudit:: dcbTransactionId = {}, err = {}", dcbTransactionId, errorMsg);
    TransactionAuditEntity transactionAuditEntityExisted = transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(dcbTransactionId).orElse(null);

    if(transactionAuditEntityExisted != null){
      TransactionAuditEntity trnAEntError = generateTrnAuditEntityFromTheFoundOneWithError(transactionAuditEntityExisted, errorMsg);
      transactionAuditRepository.save(trnAEntError);
    }
  }

  /**
   * For the case the error happens during DCB transaction creation.
   * At this time there is no transaction_audit data existed, which refers to current DCB transaction.
   * So the transaction_audit log is created with empty "before" and "after" states and the DCB transaction content is merged with the error message.
   * The exceptional case is the attempt of the DCB transaction duplication by the id (it means the TransactionEntity with such an id already exists).
   * It triggers DUPLICATE_ERROR, which is logged with the particular transaction_audit (DUPLICATE_ERROR_ACTION)
   * and refers to not existed DCB transaction (transaction_audit.transaction_id = -1)
   * */
  @Override
  public void logErrorIfTransactionAuditNotExists(String dcbTransactionId, DcbTransaction dcbTransaction, String errorMsg) {
    TransactionAuditEntity transactionAuditEntityExisted = transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(dcbTransactionId).orElse(null);
    TransactionEntity transactionEntity = transactionMapper.mapToEntity(dcbTransactionId, dcbTransaction);
    TransactionAuditEntity trnAEntError = generateTrnAuditEntityByTrnEntityWithError(dcbTransactionId, transactionEntity, errorMsg);

    if(transactionAuditEntityExisted != null){
      log.debug("logTheErrorForNotExistedTransactionAudit:: dcbTransactionId = {}, dcbTransaction = {}, err = {}", dcbTransactionId, dcbTransaction, errorMsg);
      trnAEntError.setTransactionId(DUPLICATE_ERROR_TRANSACTION_ID);
      trnAEntError.setAction(DUPLICATE_ERROR_ACTION);
    }

    transactionAuditRepository.save(trnAEntError);
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
