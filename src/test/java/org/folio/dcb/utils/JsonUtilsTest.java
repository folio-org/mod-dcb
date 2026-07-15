package org.folio.dcb.utils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.dcb.utils.JsonUtils.DESERIALIZATION_FAILURE;

import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.support.types.UnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

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

    @Test
  void jsonToObjectShouldReturnDeserializedObjectWhenJsonIsValid() {
    // TestMate-84274ea70590cf0089b37902cd75521b
    // Given
    var transactionId = "550e8400-e29b-41d4-a716-446655440000";
    var jsonString = "{\"id\":\"%s\"}".formatted(transactionId);
    var targetClass = TransactionEntity.class;
    // When
    var result = JsonUtils.jsonToObject(jsonString, targetClass);
    // Then
    assertThat(result.getId()).isEqualTo(transactionId);
  }
}
