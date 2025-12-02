package org.folio.dcb.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    assertEquals(expected, URLDecoder.decode(result, UTF_8));
  }

  @ParameterizedTest
  @CsvSource({
    "Alpha,001,'(name==\"Alpha\" AND code==\"001\")'",
    "'John Doe',X1,'(name==\"John Doe\" AND code==\"X1\")'",
    "'A*B','C D','(name==\"A\\*B\" AND code==\"C D\")'",
    "'(group)','^code?','(name==\"(group)\" AND code==\"\\^code\\?\")'"
  })
  void exactMatchByNameAndCode_positive_parameterized(String name, String code, String expected) {
    var result = CqlQuery.exactMatchByNameAndCode(name, code);
    assertEquals(expected, URLDecoder.decode(result, UTF_8));
  }

  @Test
  void createForOpenHoldRequests() {
    var itemId = "06adc710-c240-4d28-afd2-ec92c9020a56";
    var result = CqlQuery.createForOpenHoldRequests(itemId);
    var expectedQuery = "itemId==\"06adc710-c240-4d28-afd2-ec92c9020a56\"" +
      " and requestType==\"Hold\"" +
      " and status==(\"Open - Not yet filled\"" +
      " or \"Open - Awaiting pickup\"" +
      " or \"Open - In transit\"" +
      " or \"Open - Awaiting delivery\")";

    assertEquals(expectedQuery, URLDecoder.decode(result, UTF_8));
  }

  @Test
  void constructorIsPrivate() throws Exception {
    Constructor<CqlQuery> ctor = CqlQuery.class.getDeclaredConstructor(String.class);
    assertTrue(Modifier.isPrivate(ctor.getModifiers()));
    ctor.setAccessible(true);
    ctor.newInstance("test");
  }
}
