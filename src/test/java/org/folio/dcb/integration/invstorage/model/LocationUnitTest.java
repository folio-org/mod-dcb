package org.folio.dcb.integration.invstorage.model;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.UUID;
import org.folio.dcb.support.types.UnitTest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

@UnitTest
class LocationUnitTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void serializationTest() {
    var locationValue = LocationUnit.builder()
      .id(UUID.randomUUID().toString())
      .name("test")
      .isShadow(false)
      .build();

    var expectedValue = objectMapper.writeValueAsString(locationValue);
    assertFalse(expectedValue.contains("\"shadow\":false"));
  }
}
