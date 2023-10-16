package org.folio.dcb.domain.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class TransactionAuditMapper {

  private ObjectMapper objectMapper = new ObjectMapper();

  public TransactionAuditEntity mapToEntity(TransactionEntity transactionEntity) {
    if(Objects.isNull(transactionEntity)) {
      return null;
    }

    try {
      return TransactionAuditEntity.builder()
        .id(UUID.randomUUID())
        .transactionId(transactionEntity.getId())
        .before(objectMapper.writeValueAsString(transactionEntity))
        .build();
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
