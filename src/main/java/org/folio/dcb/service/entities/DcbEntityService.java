package org.folio.dcb.service.entities;

import java.util.List;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.dcb.domain.ResultList;

/**
 * Service interface for managing umbrella entities.
 *
 * @param <T> the type of the umbrella entity
 */
public interface DcbEntityService<T> {

  /**
   * Finds the umbrella entity if it exists.
   *
   * @return an {@link Optional} containing the umbrella entity, or empty if not found
   */
  Optional<T> findDcbEntity();

  /**
   * Creates a new umbrella entity.
   *
   * @return an {@link Optional} containing the created umbrella entity, or empty if creation failed
   */
  T createDcbEntity();

  /**
   * Finds the umbrella entity, or creates it if it does not exist.
   *
   * @return an {@link Optional} containing the found or created umbrella entity
   */
  default T findOrCreateEntity() {
    return findDcbEntity().orElseGet(this::createDcbEntity);
  }

  /**
   * Retrieves the first value from a {@link ResultList}.
   *
   * @param resultList the result list to search
   * @param <R> the type of the result
   * @return an {@link Optional} containing the first value, or empty if not present
   */
  default <R> Optional<R> findFirstValue(ResultList<R> resultList) {
    return Optional.ofNullable(resultList)
      .map(ResultList::getResult)
      .filter(CollectionUtils::isNotEmpty)
      .map(List::getFirst);
  }

  T getDefaultValue();
}
