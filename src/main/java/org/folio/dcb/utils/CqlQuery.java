package org.folio.dcb.utils;

import static java.util.stream.Collectors.joining;
import static org.folio.dcb.domain.dto.CirculationRequest.RequestTypeEnum.HOLD;
import static org.folio.dcb.integration.circulation.model.RequestStatus.getOpenStatuses;
import static org.folio.util.StringUtil.cqlEncode;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import org.folio.dcb.integration.circulation.model.RequestStatus;
import org.folio.util.StringUtil;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, staticName = "of")
public class CqlQuery {

  private final String query;

  /**
   * Creates a CqlQuery for an exact match on the given parameter and value.
   *
   * @param param - the CQL field to match
   * @param value - the value to match
   * @return a new CqlQuery representing the exact match
   */
  public static CqlQuery exactMatch(String param, String value) {
    return exactMatchQuery(param, value);
  }

  /**
   * Creates a CqlQuery that matches the given id.
   *
   * @param id the value to match for the "id" field
   * @return a new CqlQuery representing the match on id
   */
  public static CqlQuery exactMatchById(String id) {
    return exactMatchQuery("id", id);
  }

  /**
   * Creates a CqlQuery that matches the given name.
   *
   * @param name the value to match for the "name" field
   * @return a new CqlQuery representing the match on name
   */
  public static CqlQuery exactMatchByName(String name) {
    return exactMatchQuery("name", name);
  }

  /**
   * Creates a CqlQuery that matches the given code.
   *
   * @param name the value to match for the "name" field
   * @return a new CqlQuery representing the match on code
   */
  public static CqlQuery exactMatchByCode(String name) {
    return exactMatchQuery("code", name);
  }

  /**
   * Creates a CqlQuery that matches both the given name and code fields.
   *
   * @param name the value to match for the "name" field
   * @param code the value to match for the "code" field
   * @return a new CqlQuery representing the match on both name and code
   */
  public static CqlQuery exactMatchByNameAndCode(String name, String code) {
    return exactMatchByName(name).and(exactMatchByCode(code), true);
  }

  /**
   * Builds a CQL query string for open hold requests for a specific item.
   *
   * @param itemId the ID of the item to filter requests for
   * @return encoded CQL query string for open hold requests
   */
  public static CqlQuery openHoldRequestsQuery(String itemId) {
    return exactMatchQuery("itemId", itemId)
      .and(exactMatchQuery("requestType", HOLD.getValue()), true)
      .and(exactMatchAnyQuery("status", getOpenStatuses(), RequestStatus::getValue), true);
  }

  /**
   * Combines this CqlQuery with another using an AND operation.
   *
   * @param query the CqlQuery to combine with this one
   * @return a new CqlQuery representing the logical AND of both queries
   */
  public CqlQuery and(CqlQuery query) {
    return and(query, false);
  }

  /**
   * Combines this CqlQuery with another using an AND operation.
   * Optionally uses simplified joining without parentheses.
   *
   * @param query - the CqlQuery to combine with this one
   * @param simplifiedJoin - if true, omits parentheses around queries
   * @return a new CqlQuery representing the logical AND of both queries
   */
  public CqlQuery and(CqlQuery query, boolean simplifiedJoin) {
    return simplifiedJoin
      ? new CqlQuery("%s and %s".formatted(this.query, query.query))
      : new CqlQuery("(%s) and (%s)".formatted(this.query, query.query));
  }

  /**
   * Combines this CqlQuery with another using an AND operation.
   *
   * @param query the CqlQuery to combine with this one
   * @return a new CqlQuery representing the logical AND of both queries
   */
  public CqlQuery or(CqlQuery query) {
    return or(query, false);
  }

  /**
   * Combines this CqlQuery with another using an AND operation.
   * Optionally uses simplified joining without parentheses.
   *
   * @param query          - the CqlQuery to combine with this one
   * @param simplifiedJoin - if true, omits parentheses around queries
   * @return a new CqlQuery representing the logical AND of both queries
   */
  public CqlQuery or(CqlQuery query, boolean simplifiedJoin) {
    return simplifiedJoin
      ? new CqlQuery("%s or %s".formatted(this.query, query.query))
      : new CqlQuery("(%s) or (%s)".formatted(this.query, query.query));
  }

  /**
   * Creates a CqlQuery for an exact match on the given parameter and list of values.
   *
   * @param param  - the CQL field to match
   * @param values - the values to match
   * @return a new CqlQuery representing the exact match
   */
  public static CqlQuery exactMatchAny(String param, List<String> values) {
    var listValues = values != null ? values : List.<String>of();
    var stringValues = listValues.stream()
      .filter(StringUtils::isNotBlank)
      .map(StringUtil::cqlEncode)
      .collect(joining(" or "));
    return new CqlQuery("%s==(%s)".formatted(param, stringValues));
  }

  private static CqlQuery exactMatchQuery(String param, String value) {
    return new CqlQuery("%s==%s".formatted(param, cqlEncode(value)));
  }

  private static <T> CqlQuery exactMatchAnyQuery(String param,
    Collection<T> values, Function<T, String> stringValueMapper) {

    var stringValues = CollectionUtils.emptyIfNull(values).stream()
      .filter(Objects::nonNull)
      .map(stringValueMapper)
      .toList();
    return exactMatchAnyQuery(param, stringValues);
  }

  private static CqlQuery exactMatchAnyQuery(String param, List<String> values) {
    var stringValues = ListUtils.emptyIfNull(values).stream()
      .filter(StringUtils::isNotBlank)
      .map(StringUtil::cqlEncode)
      .collect(joining(" or "));
    return new CqlQuery("%s==(%s)".formatted(param, stringValues));
  }
}
