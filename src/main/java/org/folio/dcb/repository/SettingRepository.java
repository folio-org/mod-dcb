package org.folio.dcb.repository;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.UUID;
import org.folio.dcb.domain.entity.SettingEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingRepository extends JpaCqlRepository<SettingEntity, UUID> {

  /**
   * Returns all records if query is empty or searches using it otherwise.
   *
   * @param query - CQL query as {@link String}
   * @param pageable - {@link Pageable} object for pagination
   * @return {@link Page} containing {@link SettingEntity} records
   */
  default Page<SettingEntity> findByQuery(String query, Pageable pageable) {
    return isBlank(query) ? findAll(pageable) : findByCql(query, pageable);
  }

  /**
   * Checks if record with the specified key exists.
   *
   * @param key - the key to check for existence
   * @return true if a record with the specified key exists, false otherwise
   */
  boolean existsByKey(String key);
}
