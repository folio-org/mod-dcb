package org.folio.dcb.domain;

import static java.util.Arrays.copyOfRange;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

/**
 * Value object representing a patron's name components parsed from a localNames string.
 *
 * <p>The expected input format is a bracketed, comma-separated list with three components:
 * <pre>[{firstName}, {middleName}, {lastName}]</pre>
 * For example: <code>[John, Michael, Doe]</code>.
 *
 * <p>Use {@link #parseLocalNames(String)} to obtain an instance. If the input is blank or cannot be
 * parsed, a predefined {@link #DEFAULT_VALUE} is returned. The default last name is
 * {@link #LAST_NAME} and first/middle names are null.
 */
@Data
@Log4j2
public class DcbPersonal {

  /**
   * Default last name used when parsing fails or input is blank.
   */
  private static final String LAST_NAME = "DcbSystem";

  /**
   * Default PatronInfo returned for blank or unparseable inputs.
   */
  private static final DcbPersonal DEFAULT_VALUE = new DcbPersonal(null, null, LAST_NAME);

  private final String firstName;
  private final String middleName;
  private final String lastName;

  /**
   * Constructs a new PatronInfo instance with the given name components.
   *
   * @param firstName  the first name of the patron, may be null or blank
   * @param middleName the middle name of the patron, may be null or blank
   * @param lastName   the last name of the patron, may be null or blank
   */
  public DcbPersonal(String firstName, String middleName, String lastName) {
    this.firstName = trimToNull(firstName);
    this.middleName = trimToNull(middleName);
    this.lastName = trimToNull(lastName);
  }

  /**
   * Parse a {@code localNames} string into a {@link DcbPersonal}.
   *
   * <p>The method trims surrounding whitespace and supports the following bracketed forms:
   * <ul>
   *   <li>Three parts: [first, middle, last] — all parts required and non-blank
   *   <li>Two parts: [first, last] — middle is treated as missing (null)
   *   <li>One part: [last] — only last name is present
   * </ul>
   * If parsing fails or required parts are blank, {@link #DEFAULT_VALUE} is returned.
   *
   * @param localNames the input string to parse, may be null
   * @return a {@link DcbPersonal} with parsed components or {@link #DEFAULT_VALUE} when parsing
   * is not possible
   */
  public static DcbPersonal parseLocalNames(String localNames) {
    if (StringUtils.isBlank(localNames)) {
      return DEFAULT_VALUE;
    }

    var trimmed = localNames.trim();
    if (!(trimmed.startsWith("[") && trimmed.endsWith("]"))) {
      throw new IllegalArgumentException("Malformed localNames format. Value must start with '[' and end with ']'");
    }

    var inner = trimmed.substring(1, trimmed.length() - 1);
    if (StringUtils.isBlank(inner)) {
      return DEFAULT_VALUE;
    }

    var parts = inner.split(",", -1);
    var patronInfo = switch (parts.length) {
      case 1 -> new DcbPersonal(null, null, parts[0]);
      case 2 -> new DcbPersonal(parts[0], null, parts[1]);
      case 3 -> new DcbPersonal(parts[0], parts[1], parts[2]);
      default -> new DcbPersonal(null, null, null);
    };

    return patronInfo.isEmpty() ? DEFAULT_VALUE : patronInfo;
  }

  /**
   * Checks if all name components are null or blank.
   *
   * @return true if first, middle, and last names are all null or blank; false otherwise
   */
  public boolean isEmpty() {
    return StringUtils.isAllBlank(firstName, middleName, lastName);
  }
}
