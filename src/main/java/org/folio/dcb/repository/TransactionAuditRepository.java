package org.folio.dcb.repository;

import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionAuditRepository extends JpaRepository<TransactionAuditEntity, String> {

  @Query(value = "SELECT * FROM transactions_audit " +
    "WHERE transaction_id = :trnId " +
    "and created_date = (SELECT MAX(created_date) FROM transactions_audit WHERE transaction_id = :trnId);", nativeQuery = true)
  Optional<TransactionAuditEntity> findLatestTransactionAuditEntityByDcbTransactionId(@Param("trnId") String trnId);
}
