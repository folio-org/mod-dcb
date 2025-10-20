package org.folio.dcb.utils;

import static org.folio.util.StringUtil.cqlEncode;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.util.PercentCodec;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CqlQuery {

  /**
   * Creates a CqlQuery for an exact match on the given parameter and value.
   *
   * @param param - the CQL field to match
   * @param value - the value to match
   * @return a new CqlQuery representing the exact match
   */
  public static String exactMatch(String param, String value) {
    return String.format("%s==%s", param, encodeValue(value));
  }

  /**
   * Creates a CqlQuery that matches both the given name and code fields.
   *
   * @param name the value to match for the "name" field
   * @param code the value to match for the "code" field
   * @return a new CqlQuery representing the match on both name and code
   */
  public static String byNameAndCode(String name, String code) {
    return String.format("(name==%s AND code==%s)", encodeValue(name), encodeValue(code));
  }

  private static String encodeValue(String value) {
    return cqlEncode(PercentCodec.encode(value));
  }
}
