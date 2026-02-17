package org.folio.dcb.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.folio.dcb.support.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@UnitTest
class CqlQueryTest {

  @ParameterizedTest
  @CsvSource({
    "title,simple,'title==\"simple\"'",
    "title,'a b','title==\"a b\"'",
    "tag,'a*b','tag==\"a\\*b\"'",
    "code,'(abc)','code==\"(abc)\"'",
    "note,'space  inside','note==\"space  inside\"'",
    "sym,'^test?','sym==\"\\^test\\?\"'"
  })
  void exactMatch_positive_parameterized(String field, String value, String expected) {
    var result = CqlQuery.exactMatch(field, value);
    assertThat(result.getQuery()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "Alpha,001,'name==\"Alpha\" and code==\"001\"'",
    "'John Doe',X1,'name==\"John Doe\" and code==\"X1\"'",
    "'A*B','C D','name==\"A\\*B\" and code==\"C D\"'",
    "'(group)','^code?','name==\"(group)\" and code==\"\\^code\\?\"'"
  })
  void exactMatchByNameAndCode_positive_parameterized(String name, String code, String expected) {
    var result = CqlQuery.exactMatchByNameAndCode(name, code);
    assertThat(result.getQuery()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "Alpha,'name==\"Alpha\"'",
  })
  void exactMatchByName_parameterized(String name, String expected) {
    var result = CqlQuery.exactMatchByName(name);
    assertThat(result.getQuery()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "001,'code==\"001\"'",
  })
  void exactMatchByCode_parameterized(String code, String expected) {
    var result = CqlQuery.exactMatchByCode(code);
    assertThat(result.getQuery()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "99117f45-b091-4cc0-bd55-8d9d8efbd061,'id==\"99117f45-b091-4cc0-bd55-8d9d8efbd061\"'",
  })
  void exactMatchById_parameterized(String id, String expected) {
    var result = CqlQuery.exactMatchById(id);
    assertThat(result.getQuery()).isEqualTo(expected);
  }

  @Test
  void openHoldRequestsQuery() {
    var itemId = "06adc710-c240-4d28-afd2-ec92c9020a56";
    var result = CqlQuery.openHoldRequestsQuery(itemId);
    var expectedQuery = "itemId==\"06adc710-c240-4d28-afd2-ec92c9020a56\"" +
      " and requestType==\"Hold\"" +
      " and status==(\"Open - Not yet filled\"" +
      " or \"Open - Awaiting pickup\"" +
      " or \"Open - In transit\"" +
      " or \"Open - Awaiting delivery\")";

    assertThat(result.getQuery()).isEqualTo(expectedQuery);
  }

  @Test
  void constructorIsPrivate() throws Exception {
    Constructor<CqlQuery> ctor = CqlQuery.class.getDeclaredConstructor(String.class);
    assertThat(Modifier.isPrivate(ctor.getModifiers())).isTrue();
    ctor.setAccessible(true);
    ctor.newInstance("test");
  }

  @Test
  void exactMatchAny_positive_handlesMultipleValues() {
    var result = CqlQuery.exactMatchAny("tag", Arrays.asList("first", "second"));
    var expectedQuery = "tag==(\"first\" or \"second\")";
    assertThat(result.getQuery()).isEqualTo(expectedQuery);
  }

  @Test
  void exactMatchAny_positive_handlesNullAndBlankValues() {
    var result = CqlQuery.exactMatchAny("tag", Arrays.asList(null, "", "   "));
    var expectedQuery = "tag==()";
    assertThat(result.getQuery()).isEqualTo(expectedQuery);
  }

  @Test
  void exactMatchAny_positive_nullValue() {
    var result = CqlQuery.exactMatchAny("tag", null);
    var expectedQuery = "tag==()";
    assertThat(result.getQuery()).isEqualTo(expectedQuery);
  }

  @Test
  void and_positive_wrapsQueriesByDefault() {
    var left = CqlQuery.exactMatch("a", "1");
    var right = CqlQuery.exactMatch("b", "2");
    var result = left.and(right);

    var expectedQuery = "(a==\"1\") and (b==\"2\")";
    assertThat(result.getQuery()).isEqualTo(expectedQuery);
  }

  @Test
  void and_positive_supportsSimplifiedJoin() {
    var left = CqlQuery.exactMatch("a", "1");
    var right = CqlQuery.exactMatch("b", "2");
    var result = left.and(right, true);

    var expectedQuery = "a==\"1\" and b==\"2\"";
    assertThat(result.getQuery()).isEqualTo(expectedQuery);
  }

  @Test
  void or_positive_wrapsQueriesByDefault() {
    var left = CqlQuery.exactMatch("a", "1");
    var right = CqlQuery.exactMatch("b", "2");
    var result = left.or(right);

    var expectedQuery = "(a==\"1\") or (b==\"2\")";
    assertThat(result.getQuery()).isEqualTo(expectedQuery);
  }

  @Test
  void or_positive_supportsSimplifiedJoin() {
    var left = CqlQuery.exactMatch("a", "1");
    var right = CqlQuery.exactMatch("b", "2");
    var result = left.or(right, true);

    var expectedQuery = "a==\"1\" or b==\"2\"";
    assertThat(result.getQuery()).isEqualTo(expectedQuery);
  }
}
