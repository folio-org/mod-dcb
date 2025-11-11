package support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.repository.TransactionAuditRepository;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;

@RequiredArgsConstructor
public class AuditEntityTestVerifier {

  private static final String TRANSACTION_AUDIT_DUPLICATE_ERROR_ACTION = "DUPLICATE_ERROR";
  private static final String DUPLICATE_ERROR_TRANSACTION_ID = "-1";

  private final Map<String, Collection<String>> headers;
  private final FolioModuleMetadata folioModuleMetadata;
  private final TransactionAuditRepository repository;

  public void assertThatLatestEntityIsNotDuplicate(String id) {
    try (var ignored = new FolioExecutionContextSetter(folioModuleMetadata, headers)) {
      var auditEntity = repository.findLatestTransactionAuditEntityByDcbTransactionId(id).orElse(null);
      assertNotNull(auditEntity);
      assertNotEquals(TRANSACTION_AUDIT_DUPLICATE_ERROR_ACTION, auditEntity.getAction());
      assertNotEquals(DUPLICATE_ERROR_TRANSACTION_ID, auditEntity.getTransactionId());
    }
  }

  public TransactionAuditEntity getLatestAuditEntity(String id) {
    try (var ignored = new FolioExecutionContextSetter(folioModuleMetadata, headers)) {
      var entity = repository.findLatestTransactionAuditEntityByDcbTransactionId(id).orElse(null);
      assertNotNull(entity);
      return entity;
    }
  }

  public void assertEmpty() {
    try (var ignored = new FolioExecutionContextSetter(folioModuleMetadata, headers)) {
      var entity = repository.findAll();
      assertTrue(entity.isEmpty(), "Expected no audit entities, but some were found");
    }
  }
}
