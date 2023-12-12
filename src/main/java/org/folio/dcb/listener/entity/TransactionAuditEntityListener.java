package org.folio.dcb.listener.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.SerializationUtils;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.utils.BeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class TransactionAuditEntityListener {
  private static final String CREATE_ACTION = "CREATE";
  private static final String UPDATE_ACTION = "UPDATE";
  @Autowired
  private BeanUtil beanUtil;
  @Autowired
  private ObjectMapper objectMapper;

  @PrePersist
  public void onPrePersist(Object entity) throws JsonProcessingException {
    log.debug("onPrePersist:: creating transaction audit record");
    TransactionEntity transactionEntity = (TransactionEntity) entity;
    TransactionAuditEntity transactionAuditEntity = new TransactionAuditEntity();
    transactionAuditEntity.setAction(CREATE_ACTION);
    transactionAuditEntity.setTransactionId(transactionEntity.getId());
    transactionAuditEntity.setBefore(null);
    transactionAuditEntity.setAfter(objectMapper.writeValueAsString(transactionEntity));

    log.info("onPrePersist:: creating transaction audit record {} with action {}", transactionEntity.getId(), CREATE_ACTION);
    getEntityManager().persist(transactionAuditEntity);
  }

  @PreUpdate
  public void onPreUpdate(Object entity) throws JsonProcessingException {
    log.debug("onPreUpdate:: creating transaction audit record");
    TransactionEntity transactionEntity = (TransactionEntity) entity;
    TransactionAuditEntity transactionAuditEntity = new TransactionAuditEntity();
    transactionAuditEntity.setBefore(objectMapper.writeValueAsString(transactionEntity.getSavedState()));
    transactionAuditEntity.setAfter(objectMapper.writeValueAsString(transactionEntity));
    transactionAuditEntity.setTransactionId(transactionEntity.getId());
    transactionAuditEntity.setAction(UPDATE_ACTION);

    log.info("onPreUpdate:: creating transaction audit record {} with action {}", transactionEntity.getId(), UPDATE_ACTION);
    getEntityManager().persist(transactionAuditEntity);
  }

  //This method will be invoked when the transactionEntity is loaded and the transactionEntity is stored in a transient field
  //The stored value will be used in onPreUpdate method's setBefore method.
  @PostLoad
  public void saveState(TransactionEntity transactionEntity){
    transactionEntity.setSavedState(SerializationUtils.clone((transactionEntity)));
  }

  //EntityListeners are instantiated by JPA, not Spring,
  //So Spring cannot inject any Spring-managed bean directly, e.g. EntityManager in any EntityListeners.
  private EntityManager getEntityManager() {
    return beanUtil.getBean(EntityManager.class);
  }

}
