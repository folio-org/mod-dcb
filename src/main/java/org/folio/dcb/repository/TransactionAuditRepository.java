package org.folio.dcb.repository;

import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionAuditRepository extends JpaRepository<TransactionAuditEntity, UUID> {
}
