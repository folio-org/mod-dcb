package org.folio.dcb.listener.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.service.impl.TransactionsServiceImpl;
import org.folio.dcb.utils.BeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@Log4j2
public class TransactionAuditEntityListener {
  private static final String CREATE_ACTION = "CREATE";
  private static final String UPDATE_ACTION = "UPDATE";

  @Autowired
  private BeanUtil beanUtil;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private TransactionAuditEntity transactionAuditEntity;
  private static final Map<String, TransactionEntity> originalStateCache = new HashMap<>();

  @PrePersist
  public void onPrePersist(Object entity) throws JsonProcessingException {
    log.debug("onPrePersist:: creating transaction audit record");
    TransactionEntity transactionEntity = (TransactionEntity) entity;
    transactionAuditEntity = new TransactionAuditEntity();
    transactionAuditEntity.setAction(CREATE_ACTION);
    transactionAuditEntity.setTransactionId(transactionEntity.getId());
    transactionAuditEntity.setBefore(null);
    transactionAuditEntity.setAfter(objectMapper.writeValueAsString(transactionEntity));

    log.info("onPrePersist:: creating transaction audit record {} with action {}", transactionEntity.getId(), CREATE_ACTION);
    getEntityManager().persist(transactionAuditEntity);
  }

  @PostPersist
  public void onPostPersist(Object entity) {
    TransactionEntity transactionEntity = (TransactionEntity) entity;
    originalStateCache.put(transactionEntity.getId(), transactionEntity);
  }

  @PreUpdate
  public void onPreUpdate(Object entity) throws JsonProcessingException {
    log.debug("onPreUpdate:: creating transaction audit record");
    TransactionEntity transactionEntity = (TransactionEntity) entity;
    transactionAuditEntity = new TransactionAuditEntity();
    transactionAuditEntity.setBefore(objectMapper.writeValueAsString(getTransactionEntity(transactionEntity.getId())));
    transactionAuditEntity.setAfter(objectMapper.writeValueAsString(transactionEntity));
    transactionAuditEntity.setTransactionId(transactionEntity.getId());
    transactionAuditEntity.setAction(UPDATE_ACTION);

    log.info("onPreUpdate:: creating transaction audit record {} with action {}", transactionEntity.getId(), UPDATE_ACTION);
    getEntityManager().persist(transactionAuditEntity);
    originalStateCache.put(transactionEntity.getId(), transactionEntity);
  }

  private TransactionEntity getTransactionEntity(String transactionId) {
    // Try to get the original state from the cache
    TransactionEntity originalState = originalStateCache.get(transactionId);

    // If not found, fetch it from the database
    if (Objects.isNull(originalState)) {
      originalState = getTransactionsService().getTransactionEntityOrThrow(transactionId);
      originalStateCache.put(transactionId, originalState);
    }
    return originalState;
  }

  //EntityListeners are instantiated by JPA, not Spring,
  //So Spring cannot inject any Spring-managed bean directly, e.g. EntityManager in any EntityListeners.
  private EntityManager getEntityManager() {
    return beanUtil.getBean(EntityManager.class);
  }

  private TransactionsServiceImpl getTransactionsService() {
    return beanUtil.getBean(TransactionsServiceImpl.class);
  }
}
