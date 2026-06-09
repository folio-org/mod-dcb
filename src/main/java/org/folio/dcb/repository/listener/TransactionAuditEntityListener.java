package org.folio.dcb.repository.listener;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.SerializationUtils;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.utils.BeanUtil;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@Component
@RequiredArgsConstructor
public class TransactionAuditEntityListener {

  private static final String CREATE_ACTION = "CREATE";
  private static final String UPDATE_ACTION = "UPDATE";

  private final BeanUtil beanUtil;
  private final ObjectMapper objectMapper;

  @PrePersist
  public void onPrePersist(Object entity) throws JacksonException {
    log.debug("onPrePersist:: creating transaction audit record");
    var txEntity = (TransactionEntity) entity;
    var transactionAuditEntity = new TransactionAuditEntity();
    transactionAuditEntity.setAction(CREATE_ACTION);
    transactionAuditEntity.setTransactionId(txEntity.getId());
    transactionAuditEntity.setBefore(null);
    transactionAuditEntity.setAfter(objectMapper.writeValueAsString(txEntity));

    log.info("onPrePersist:: creating transaction audit record {} with action {}", txEntity.getId(), CREATE_ACTION);
    getEntityManager().persist(transactionAuditEntity);
  }

  @PreUpdate
  public void onPreUpdate(Object entity) throws JacksonException {
    log.debug("onPreUpdate:: creating transaction audit record");
    var txEntity = (TransactionEntity) entity;
    var transactionAuditEntity = new TransactionAuditEntity();
    transactionAuditEntity.setBefore(objectMapper.writeValueAsString(txEntity.getSavedState()));
    transactionAuditEntity.setAfter(objectMapper.writeValueAsString(txEntity));
    transactionAuditEntity.setTransactionId(txEntity.getId());
    transactionAuditEntity.setAction(UPDATE_ACTION);

    log.info("onPreUpdate:: creating transaction audit record {} with action {}", txEntity.getId(), UPDATE_ACTION);
    getEntityManager().persist(transactionAuditEntity);
  }

  /**
   * Saves entity state.
   *
   * <p>This method will be invoked when the transactionEntity is loaded and the transactionEntity is stored in a
   * transient field. The stored value will be used in onPreUpdate method's setBefore method.</p>
   *
   * @param transactionEntity the transaction entity being loaded
   */
  @PostLoad
  public void saveState(TransactionEntity transactionEntity) {
    transactionEntity.setSavedState(SerializationUtils.clone(transactionEntity));
  }

  /**
   * Gets the entity manager bean from the application context.
   *
   * <p>
   * EntityListeners are instantiated by JPA, not Spring, so Spring cannot inject any Spring-managed bean directly, e.g.
   * EntityManager in any EntityListeners.
   * </p>
   *
   * @return the entity manager instance
   */
  private EntityManager getEntityManager() {
    return beanUtil.getBean(EntityManager.class);
  }
}
