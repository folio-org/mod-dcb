package org.folio.dcb.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public class JsonTestUtils {

  protected static ObjectMapper objectMapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  @SneakyThrows
  public static String asJsonString(Object value) {
    return objectMapper.writeValueAsString(value);
  }

  @SneakyThrows
  public static JsonNode toJsonNode(Object value) {
    return objectMapper.valueToTree(value);
  }
}
