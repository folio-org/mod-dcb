package org.folio.dcb.repository;

import org.folio.dcb.domain.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
  @Query(value = "SELECT * FROM transactions where item_id = :itemId AND status != 'CLOSED'", nativeQuery = true)
  Optional<TransactionEntity> findTransactionByItemIdAndStatusNotInClosed(@Param("itemId") UUID itemId);

}
