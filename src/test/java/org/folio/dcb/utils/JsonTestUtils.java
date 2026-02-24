package org.folio.dcb.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import tools.jackson.databind.json.JsonMapper;

public class JsonTestUtils {

  public static final JsonMapper JSON_MAPPER = JsonMapper.builder()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
    .build();

  @SneakyThrows
  public static String asJsonString(Object value) {
    return JSON_MAPPER.writeValueAsString(value);
  }

  @SneakyThrows
  public static JsonNode toJsonNode(Object value) {
    return JSON_MAPPER.valueToTree(value);
  }
}
