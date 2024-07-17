package org.folio.dcb.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@UtilityClass
@Log4j2
public class JsonUtils {
  public static final String DESERIALIZATION_FAILURE = "Failed to deserialize json string";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static <T> T jsonToObject(String jsonString, Class<T> classToDeserialize) {
    try {
      return OBJECT_MAPPER.readValue(jsonString, classToDeserialize);
    } catch (JsonProcessingException ex) {
      log.info(DESERIALIZATION_FAILURE + ex);
      throw new IllegalArgumentException(DESERIALIZATION_FAILURE + ex.getMessage());
    }
  }

}
