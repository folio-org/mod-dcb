package org.folio.dcb.utils;

import static java.util.stream.Collectors.joining;
import static org.folio.dcb.domain.dto.CirculationRequest.RequestTypeEnum.HOLD;
import static org.folio.dcb.utils.RequestStatus.getOpenStatuses;
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
import lombok.NoArgsConstructor;
import org.folio.util.PercentCodec;
import org.folio.util.StringUtil;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, staticName = "of")
public class CqlQuery {

  private final String query;

  /**
   * Creates a CqlQuery that matches both the given id.
   *
   * @param id the value to match for the "id" field
   * @return a new CqlQuery representing the match on both name and code
   */
  public static String exactMatchById(String id) {
    return exactMatchQuery("id", id).toText();
  }

  /**
   * Creates a CqlQuery that matches both the given name and code fields.
   *
   * @param name the value to match for the "name" field
   * @param code the value to match for the "code" field
   * @return a new CqlQuery representing the match on both name and code
   */
  public static String exactMatchByNameAndCode(String name, String code) {
    var query = String.format("(name==%s AND code==%s)", cqlEncode(name), cqlEncode(code));
    return PercentCodec.encodeAsString(query);
  }

  /**
   * Builds a CQL query string for open hold requests for a specific item.
   *
   * @param itemId the ID of the item to filter requests for
   * @return encoded CQL query string for open hold requests
   */
  public static String createForOpenHoldRequests(String itemId) {
    return exactMatchQuery("itemId", itemId)
      .and(exactMatchQuery("requestType", HOLD.getValue()), true)
      .and(exactMatchAnyQuery("status", getOpenStatuses(), RequestStatus::getValue), true)
      .toText();
  }

  /**
   * Creates a CqlQuery for an exact match on the given parameter and value.
   *
   * @param param - the CQL field to match
   * @param value - the value to match
   * @return a new CqlQuery representing the exact match
   */
  public static String exactMatch(String param, String value) {
    return exactMatchQuery(param, value).toText();
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
   * Returns the encoded CQL query string representation of this query.
   *
   * @return encoded CQL query string
   */
  public String toText() {
    return PercentCodec.encodeAsString(this.query);
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
