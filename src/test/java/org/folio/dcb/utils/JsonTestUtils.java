package org.folio.dcb.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import tools.jackson.databind.json.JsonMapper;

public class JsonTestUtils {

  protected static ObjectMapper objectMapper = JsonMapper.builder()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
    .build();

  @SneakyThrows
  public static String asJsonString(Object value) {
    return objectMapper.writeValueAsString(value);
  }

  @SneakyThrows
  public static JsonNode toJsonNode(Object value) {
    return objectMapper.valueToTree(value);
  }
}
