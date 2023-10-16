package org.folio.dcb.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.mapper.TransactionAuditMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionAuditMapperTest {

  @InjectMocks
  TransactionAuditMapper transactionAuditMapper;
  @Mock
  ObjectMapper objectMapper;

  @Test
  void mapToEntityTest() {
    TransactionEntity transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);

    TransactionAuditEntity transactionAudit = transactionAuditMapper.mapToEntity(transactionEntity);
    assertNotNull(transactionAudit);
  }

  @Test
  void mapToEntityNullTest() {
    TransactionAuditEntity transactionAudit = transactionAuditMapper.mapToEntity(null);

    assertNull(transactionAudit);
  }

  @Test
  void mapToEntityErrorTest() throws JsonProcessingException {
    TransactionEntity transactionEntity = new TransactionEntity();
    when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);

    assertThrows(IllegalArgumentException.class, () -> transactionAuditMapper.mapToEntity(transactionEntity));
  }
}
