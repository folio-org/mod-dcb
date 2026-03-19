package org.folio.dcb.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonUtilsTest {

  @Test
  void jsonToObject_returnsMappedObject() {
    var json = "{\"name\":\"Ada\",\"age\":42}";

    var person = JsonUtils.jsonToObject(json, Person.class);

    assertThat(person).isNotNull();
    assertThat(person.name).isEqualTo("Ada");
    assertThat(person.age).isEqualTo(42);
  }

  @Test
  void jsonToObject_throwsWhenJsonInvalid() {
    var json = "{\"name\":}";

    assertThatThrownBy(() -> JsonUtils.jsonToObject(json, Person.class))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(JsonUtils.DESERIALIZATION_FAILURE);
  }

  @Test
  void readTree_returnsJsonNode() {
    JsonNode node = JsonUtils.readTree("{\"a\":1}");

    assertThat(node.get("a").asInt()).isEqualTo(1);
  }

  @Test
  void readTree_throwsWhenJsonInvalid() {
    assertThatThrownBy(() -> JsonUtils.readTree("{"))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("Failed to read string value")
      .hasCauseInstanceOf(JsonProcessingException.class);
  }

  @Test
  void writeValueAsString_returnsJsonString() {
    var json = JsonUtils.writeValueAsString(Map.of("key", "value"));

    assertThat(json).isEqualTo("{\"key\":\"value\"}");
  }

  @Test
  void writeValueAsString_throwsWhenSerializationFails() {
    assertThatThrownBy(() -> JsonUtils.writeValueAsString(new SelfRef()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Failed to convert value to JSON string")
      .hasCauseInstanceOf(JsonProcessingException.class);
  }

  static class Person {
    public String name;
    public int age;

    Person() {}

    Person(String name, int age) {
      this.name = name;
      this.age = age;
    }
  }

  static class SelfRef {
    public SelfRef getSelf() {
      return this;
    }
  }
}

