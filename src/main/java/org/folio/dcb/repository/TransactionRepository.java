package org.folio.dcb.repository;

import org.folio.dcb.domain.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
  @Query(value = "SELECT * FROM transactions where item_id = :itemId AND status not in ('CLOSED', 'CANCELLED', 'ERROR')", nativeQuery = true)
  Optional<TransactionEntity> findTransactionByItemIdAndStatusNotInClosed(@Param("itemId") UUID itemId);

  @Query(value = "SELECT * FROM transactions where item_id = :itemId AND status not in ('CLOSED', 'CANCELLED', 'ERROR')", nativeQuery = true)
  List<TransactionEntity> findTransactionsByItemIdAndStatusNotInClosed(@Param("itemId") UUID itemId);

  @Query(value = "SELECT * FROM transactions where request_id = :requestId AND status != 'CLOSED'", nativeQuery = true)
  Optional<TransactionEntity> findTransactionByRequestIdAndStatusNotInClosed(@Param("requestId") UUID itemId);

  @Query("SELECT COUNT(t) FROM transactions t WHERE t.updatedDate >= :fromDate AND t.updatedDate <= :toDate")
  int countTransactionsByDateRange(OffsetDateTime fromDate, OffsetDateTime toDate);

  @Query("SELECT t FROM transactions t WHERE t.updatedDate >= :fromDate AND t.updatedDate <= :toDate")
  Page<TransactionEntity> findTransactionsByDateRange(OffsetDateTime fromDate, OffsetDateTime toDate, Pageable pageable);

}
