package org.folio.dcb.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CqlQueryTest {

  @ParameterizedTest
  @CsvSource({
    "title,simple,'title==\"simple\"'",
    "title,'a b','title==\"a%20b\"'",
    "tag,'a*b','tag==\"a%2Ab\"'",
    "code,'(abc)','code==\"%28abc%29\"'",
    "note,'space  inside','note==\"space%20%20inside\"'",
    "sym,'^test?','sym==\"%5Etest%3F\"'"
  })
  void exactMatch_positive_parameterized(String field, String value, String expected) {
    assertEquals(expected, CqlQuery.exactMatch(field, value));
  }

  @ParameterizedTest
  @CsvSource({
    "Alpha,001,'(name==\"Alpha\" AND code==\"001\")'",
    "'John Doe',X1,'(name==\"John%20Doe\" AND code==\"X1\")'",
    "'A*B','C D','(name==\"A%2AB\" AND code==\"C%20D\")'",
    "'(group)','^code?','(name==\"%28group%29\" AND code==\"%5Ecode%3F\")'"
  })
  void byNameAndCode_positive_parameterized(String name, String code, String expected) {
    assertEquals(expected, CqlQuery.byNameAndCode(name, code));
  }

  @Test
  void constructorIsPrivate() throws Exception {
    Constructor<CqlQuery> ctor = CqlQuery.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(ctor.getModifiers()));
    ctor.setAccessible(true);
    ctor.newInstance();
  }
}
