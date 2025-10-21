package org.folio.dcb.client.feign;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.UUID;
import org.folio.dcb.client.feign.LocationUnitClient.LocationUnit;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

class LocationUnitClientTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void serializationTest() throws JsonProcessingException {
    var locationValue = LocationUnit.builder()
      .id(UUID.randomUUID().toString())
      .name("test")
      .isShadow(false)
      .build();

    var expectedValue = objectMapper.writeValueAsString(locationValue);
    assertFalse(expectedValue.contains("\"shadow\":false"));
  }
}
