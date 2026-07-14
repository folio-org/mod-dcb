package org.folio.dcb.utils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.dcb.utils.JsonUtils.DESERIALIZATION_FAILURE;

import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.support.types.UnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class JsonUtilsTest {

  @ParameterizedTest
  @ValueSource(strings = {
    "{invalid:json}",
    "",
    "{\"id\": []}"
  })
  void jsonToObjectShouldThrowIllegalArgumentExceptionWhenDeserializationFails(String invalidJson) {
    // TestMate-c47cc79d469f3ffa287a5a8d01a25847
    var targetClass = TransactionEntity.class;
    assertThatThrownBy(() -> JsonUtils.jsonToObject(invalidJson, targetClass))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith(DESERIALIZATION_FAILURE);
  }
}
