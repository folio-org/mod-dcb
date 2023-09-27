package org.folio.dcb.repository;

import org.folio.dcb.domain.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

  Optional<TransactionEntity> findTransactionByItemId(String itemId);

}
