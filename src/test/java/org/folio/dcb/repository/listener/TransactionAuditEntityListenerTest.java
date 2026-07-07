package org.folio.dcb.repository.listener;

import jakarta.persistence.EntityManager;
import org.folio.dcb.support.types.UnitTest;
import org.folio.dcb.utils.BeanUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.core.JacksonException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TransactionAuditEntityListenerTest {

  @InjectMocks
  private TransactionAuditEntityListener transactionAuditEntityListener;

  @Mock
  private BeanUtil beanUtil;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private EntityManager entityManager;

    @Test
  void onPrePersistShouldCreateAndPersistAuditRecord() throws JacksonException {
    // TestMate-bf56de4ad8708b9afd2934004879a737
    // Given
    String transactionId = "tx-123";
    String expectedJson = "{\"id\":\"tx-123\"}";
    TransactionEntity transactionEntity = TransactionEntity.builder()
      .id(transactionId)
      .build();
    when(beanUtil.getBean(EntityManager.class)).thenReturn(entityManager);
    when(objectMapper.writeValueAsString(transactionEntity)).thenReturn(expectedJson);
    // When
    transactionAuditEntityListener.onPrePersist(transactionEntity);
    // Then
    ArgumentCaptor<TransactionAuditEntity> auditCaptor = ArgumentCaptor.forClass(TransactionAuditEntity.class);
    verify(entityManager).persist(auditCaptor.capture());
    TransactionAuditEntity capturedAudit = auditCaptor.getValue();
    assertThat(capturedAudit.getAction()).isEqualTo("CREATE");
    assertThat(capturedAudit.getTransactionId()).isEqualTo(transactionId);
    assertThat(capturedAudit.getBefore()).isNull();
    assertThat(capturedAudit.getAfter()).isEqualTo(expectedJson);
  }

    @Test
  void onPrePersistShouldPropagateJacksonExceptionWhenSerializationFails() throws JacksonException {
    // TestMate-cef71670364a044a6369cb627a4e6f24
    // Given
    String transactionId = "tx-failure-123";
    TransactionEntity transactionEntity = TransactionEntity.builder()
      .id(transactionId)
      .build();
    JacksonException jacksonException = mock(JacksonException.class);
    // The call to getEntityManager() (which uses beanUtil) happens after writeValueAsString.
    // If writeValueAsString throws an exception, beanUtil is never invoked, making the stubbing unnecessary.
    when(objectMapper.writeValueAsString(transactionEntity)).thenThrow(jacksonException);
    // When
    assertThrows(JacksonException.class, () -> transactionAuditEntityListener.onPrePersist(transactionEntity));
    // Then
    verify(entityManager, never()).persist(any());
  }

}
