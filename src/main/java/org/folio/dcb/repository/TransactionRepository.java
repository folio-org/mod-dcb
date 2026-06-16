package org.folio.dcb.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

  @Query(nativeQuery = true, value = """
    SELECT * FROM transactions
    WHERE item_id = :itemId
      AND status NOT IN ('CLOSED', 'CANCELLED', 'ERROR', 'CREATED', 'OPEN')
    """)
  Optional<TransactionEntity> findTransactionByItemIdAndStatusNotInClosed(@Param("itemId") UUID itemId);

  @Query(nativeQuery = true, value = """
    SELECT * FROM transactions
    WHERE item_id = :itemId AND status NOT IN ('CLOSED', 'CANCELLED', 'ERROR', 'EXPIRED')
    """)
  List<TransactionEntity> findTransactionsByItemIdAndStatusNotInClosed(@Param("itemId") UUID itemId);

  @Query(nativeQuery = true, value = "SELECT * FROM transactions WHERE item_id = :itemId AND status = 'EXPIRED'")
  List<TransactionEntity> findExpiredTransactionsByItemId(@Param("itemId") UUID itemId);

  @Query(nativeQuery = true, value =
    "SELECT * FROM transactions WHERE item_id = :itemId AND status NOT IN ('CLOSED', 'CANCELLED', 'ERROR')")
  Optional<TransactionEntity> findSingleTransactionsByItemIdAndStatusNotInClosed(@Param("itemId") UUID itemId);

  @Query(nativeQuery = true, value = "SELECT * FROM transactions WHERE request_id = :requestId AND status != 'CLOSED'")
  Optional<TransactionEntity> findTransactionByRequestIdAndStatusNotInClosed(@Param("requestId") UUID itemId);
}
