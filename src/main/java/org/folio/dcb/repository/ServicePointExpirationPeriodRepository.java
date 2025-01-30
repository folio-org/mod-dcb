package org.folio.dcb.repository;

import org.folio.dcb.domain.entity.ServicePointExpirationPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicePointExpirationPeriodRepository extends
  JpaRepository<ServicePointExpirationPeriodEntity, String> {
}
