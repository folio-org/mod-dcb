package org.folio.dcb.repository.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.support.types.UnitTest;
import org.folio.dcb.utils.BeanUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TransactionAuditEntityListenerTest {

  @InjectMocks private TransactionAuditEntityListener entityListener;
  @Mock private BeanUtil beanUtil;
  @Mock private ObjectMapper objectMapper;
  @Mock private EntityManager entityManager;

  @Test
  void onPrePersistShouldCreateAndPersistAuditRecord() throws JacksonException {
    // TestMate-bf56de4ad8708b9afd2934004879a737
    var transactionId = UUID.randomUUID().toString();
    var expectedJson = "{\"id\":\"%s\"}".formatted(transactionId);
    var transactionEntity = TransactionEntity.builder().id(transactionId).build();
    var auditCaptor = ArgumentCaptor.forClass(TransactionAuditEntity.class);

    doNothing().when(entityManager).persist(auditCaptor.capture());
    when(beanUtil.getBean(EntityManager.class)).thenReturn(entityManager);
    when(objectMapper.writeValueAsString(transactionEntity)).thenReturn(expectedJson);

    entityListener.onPrePersist(transactionEntity);

    var capturedAudit = auditCaptor.getValue();
    assertThat(capturedAudit.getAction()).isEqualTo("CREATE");
    assertThat(capturedAudit.getTransactionId()).isEqualTo(transactionId);
    assertThat(capturedAudit.getBefore()).isNull();
    assertThat(capturedAudit.getAfter()).isEqualTo(expectedJson);
  }

  @Test
  void onPrePersistShouldPropagateJacksonExceptionWhenSerializationFails() throws JacksonException {
    // TestMate-cef71670364a044a6369cb627a4e6f24
    var transactionId = UUID.randomUUID().toString();
    var transactionEntity = TransactionEntity.builder().id(transactionId).build();
    var jacksonException = mock(JacksonException.class);
    // The call to getEntityManager() (which uses beanUtil) happens after writeValueAsString.
    // If writeValueAsString throws an exception, beanUtil is never invoked, making the stubbing unnecessary.
    when(objectMapper.writeValueAsString(transactionEntity)).thenThrow(jacksonException);
    assertThatThrownBy(() -> entityListener.onPrePersist(transactionEntity))
      .isInstanceOf(JacksonException.class);
    verify(entityManager, never()).persist(any());
  }
}
