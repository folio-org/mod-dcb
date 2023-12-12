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
    TransactionAuditEntity auditExisting = transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(dcbTransactionId).orElse(null);

    if (auditExisting != null) {
      TransactionAuditEntity auditError = generateTrnAuditEntityFromTheFoundOneWithError(auditExisting, errorMsg);
      transactionAuditRepository.save(auditError);
    }
  }

  /**
   * For the case the error happens during DCB transaction creation.
   * At this time there is no transaction_audit data persisted, which refers to current DCB transaction.
   * So the transaction_audit log is created with empty "before" and "after" states and the DCB transaction content is merged with the error message.
   * The exceptional case is the attempt of the DCB transaction duplication by the id (it means the TransactionEntity with such an id already exists).
   * It triggers DUPLICATE_ERROR, which is logged with the particular transaction_audit (DUPLICATE_ERROR_ACTION)
   * and refers to not existed DCB transaction (transaction_audit.transaction_id = -1)
   * */
  @Override
  public void logErrorIfTransactionAuditNotExists(String dcbTransactionId, DcbTransaction dcbTransaction, String errorMsg) {
    TransactionAuditEntity auditExisting = transactionAuditRepository.findLatestTransactionAuditEntityByDcbTransactionId(dcbTransactionId).orElse(null);
    TransactionEntity transactionMapped = transactionMapper.mapToEntity(dcbTransactionId, dcbTransaction);
    TransactionAuditEntity auditError = generateTrnAuditEntityByTrnEntityWithError(dcbTransactionId, transactionMapped, errorMsg);

    if (auditExisting != null) {
      log.debug("logTheErrorForNotExistedTransactionAudit:: dcbTransactionId = {}, dcbTransaction = {}, err = {}", dcbTransactionId, dcbTransaction, errorMsg);
      auditError.setTransactionId(DUPLICATE_ERROR_TRANSACTION_ID);
      auditError.setAction(DUPLICATE_ERROR_ACTION);
    }

    transactionAuditRepository.save(auditError);
  }

  private TransactionAuditEntity generateTrnAuditEntityFromTheFoundOneWithError(TransactionAuditEntity existed, String errorMsg) {
    TransactionAuditEntity auditError = new TransactionAuditEntity();
    auditError.setId(UUID.randomUUID());
    auditError.setTransactionId(existed.getTransactionId());
    auditError.setAction(ERROR_ACTION);
    auditError.setBefore(existed.getAfter());
    auditError.setAfter(existed.getAfter());
    auditError.setErrorMessage(errorMsg);

    return auditError;
  }

  private TransactionAuditEntity generateTrnAuditEntityByTrnEntityWithError(String dcbTransactionId, TransactionEntity trnE, String errorMsg) {
    String errorMessage = String.format("dcbTransactionId = %s; dcb transaction content = %s; error message = %s.", dcbTransactionId, trnE.toString(), errorMsg);

    TransactionAuditEntity auditError = new TransactionAuditEntity();
    auditError.setId(UUID.randomUUID());
    auditError.setTransactionId(dcbTransactionId);
    auditError.setAction(ERROR_ACTION);
    auditError.setBefore(null);
    auditError.setAfter(null);
    auditError.setErrorMessage(errorMessage);

    return auditError;
  }
}
