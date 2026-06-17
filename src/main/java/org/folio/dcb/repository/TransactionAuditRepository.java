package org.folio.dcb.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionAuditRepository extends JpaRepository<TransactionAuditEntity, String> {

  @Query(nativeQuery = true,
    value = """
      SELECT * FROM transactions_audit
      WHERE transaction_id = :trnId
        AND created_date = (
          SELECT MAX(created_date) FROM transactions_audit
          WHERE transaction_id = :trnId
        )
      """)
  Optional<TransactionAuditEntity> findLatestTransactionAuditEntityByDcbTransactionId(@Param("trnId") String trnId);

  @Query(nativeQuery = true,
    value = """
      SELECT * FROM transactions_audit t
      WHERE t.created_date >= :fromDate
        AND t.created_date <= :toDate
        AND t.action = 'UPDATE'
      """,
    countQuery = """
      SELECT COUNT(*) FROM transactions_audit t
      WHERE t.created_date >= :fromDate
        AND t.created_date <= :toDate
        AND t.action = 'UPDATE'
      """)
  Page<TransactionAuditEntity> findUpdatedTransactionsByDateRange(OffsetDateTime fromDate, OffsetDateTime toDate,
    Pageable pageable);
}
