package org.folio.dcb.integration.kafka;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.utils.JsonTestUtils.JSON_MAPPER;
import static org.folio.dcb.utils.JsonTestUtils.toJsonNode;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.dcb.integration.kafka.model.EventData;
import org.folio.dcb.integration.kafka.model.KafkaEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;
import tools.jackson.databind.JsonNode;

@ExtendWith(MockitoExtension.class)
class TransactionHelperTest {

  @Test
  void getHeaderValue_positive_shouldReturnHeaderValueWhenExists() {
    // TestMate-2d5040ffc335eec4a8a7920946d099b8
    var headerName = "x-okapi-tenant";
    var headerValue = "diku";
    var defaultValue = "default-tenant";
    var headers = new MessageHeaders(Map.of(headerName, headerValue.getBytes(StandardCharsets.UTF_8)));

    var result = TransactionHelper.getHeaderValue(headers, headerName, defaultValue);

    assertThat(result).hasSize(1).containsExactly(headerValue);
  }

  @Test
  void getHeaderValue_positive_shouldReturnDefaultValueWhenHeaderMissing() {
    // TestMate-0d2c51271449a74c5b6391cb2241f2a8
    var headerName = "missing-header";
    var defaultValue = "fallback";
    var headers = new MessageHeaders(emptyMap());

    var result = TransactionHelper.getHeaderValue(headers, headerName, defaultValue);

    assertThat(result).hasSize(1).containsExactly(defaultValue);
  }

  @Test
  void getHeaderValue_positive_shouldReturnEmptyListWhenBothValueAndDefaultAreNull() {
    // TestMate-4423ae29a46220bb8c5e8ca0ce09bb97
    var headerName = "non-existent-header";
    var headers = new MessageHeaders(emptyMap());
    var result = TransactionHelper.getHeaderValue(headers, headerName, null);
    assertThat(result).isEmpty();
  }

  @Test
  void getHeaderValue_positive_shouldHandleEmptyByteArrayHeader() {
    // TestMate-bfbbbd329f6a2dc1d12c169998db4db5
    var headerName = "test-header";
    var defaultValue = "default";
    var emptyHeaderValue = new byte[0];
    var headers = new MessageHeaders(Map.of(headerName, emptyHeaderValue));

    var result = TransactionHelper.getHeaderValue(headers, headerName, defaultValue);

    assertThat(result).hasSize(1).containsExactly("");
  }

  @Test
  void getHeaderValue_positive_shouldHandleSpecialCharactersInUtf8() {
    // TestMate-75141cc3fdff80ada4b6ce4d76e31673
    var headerName = "special-header";
    var headerValue = "tést-vålue";
    var defaultValue = "default";
    var headers = new MessageHeaders(Map.of(headerName, headerValue.getBytes(StandardCharsets.UTF_8)));

    var result = TransactionHelper.getHeaderValue(headers, headerName, defaultValue);

    assertThat(result).hasSize(1).containsExactly(headerValue);
  }

  @Test
  void parseLoanEventShouldMapCheckOutAction() {
    var itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    var payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "action": "checkedout"
          }
        }
      }
      """.formatted(itemId);

    var result = TransactionHelper.parseLoanEvent(payload);

    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(EventData.EventType.CHECK_OUT);
  }

  @Test
  void parseLoanEventShouldMapCheckInAction() {
    var itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    var payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "action": "checkedin"
          }
        }
      }
      """.formatted(itemId);

    var result = TransactionHelper.parseLoanEvent(payload);

    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(EventData.EventType.CHECK_IN);
  }

  @Test
  void parseLoanEventShouldReturnNullTypeForUnknownAction() {
    var itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    var payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "action": "other"
          }
        }
      }
      """.formatted(itemId);

    var result = TransactionHelper.parseLoanEvent(payload);

    assertThat(result).isNotNull();
    assertThat(result.getType()).isNull();
  }

  @MethodSource("dcbFlagEventPayloadDataSource")
  @DisplayName("parseLoanEvent_parameterized_dcbCheck")
  @ParameterizedTest(name = "[{index}] {0}")
  void parseLoanEvent_parameterized_dcbCheck(
    @SuppressWarnings("unused") String name, String payload, boolean expected) {
    var result = TransactionHelper.parseLoanEvent(payload);

    assertThat(result).isNotNull()
      .extracting(EventData::isDcb)
      .isEqualTo(expected);
  }

  @Test
  void parseLoanEventShouldMapCheckInFoundByLibraryAction() {
    var itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    var payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "action": "checkedInFoundByLibrary"
          }
        }
      }
      """.formatted(itemId);

    var result = TransactionHelper.parseLoanEvent(payload);

    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(EventData.EventType.CHECK_IN);
    assertThat(result.getClaimedReturnedResolution()).isEqualTo("Found by library");
  }

  @Test
  void parseLoanEventShouldMapCheckInReturnedByPatronAction() {
    var itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    var payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "action": "checkedInReturnedByPatron"
          }
        }
      }
      """.formatted(itemId);

    var result = TransactionHelper.parseLoanEvent(payload);

    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(EventData.EventType.CHECK_IN);
    assertThat(result.getClaimedReturnedResolution()).isEqualTo("Returned by patron");
  }

  @Test
  void parseLoanEventShouldNotSetClaimedReturnedResolutionForRegularCheckIn() {
    var itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    var payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "action": "checkedin"
          }
        }
      }
      """.formatted(itemId);

    var result = TransactionHelper.parseLoanEvent(payload);

    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(EventData.EventType.CHECK_IN);
    assertThat(result.getClaimedReturnedResolution()).isNull();
  }

  @Test
  void parseLoanEventShouldPopulateLoanStatusWhenPresent() {
    // TestMate-bcbe206fc559de7d897da32b27a7e5ab
    var itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    var expectedStatus = "Open - Checked out";
    var payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "status": {
              "name": "%s"
            }
          }
        }
      }
      """.formatted(itemId, expectedStatus);

    var result = TransactionHelper.parseLoanEvent(payload);
    assertThat(result).isNotNull();
    assertThat(result.getItemId()).isEqualTo(itemId);
    assertThat(result.getLoanStatus()).isEqualTo(expectedStatus);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"type\": \"UPDATED\"}",
    "{\"type\": \"UPDATED\", \"data\": {\"new\": null}}",
    "{\"type\": \"UPDATED\", \"data\": {\"new\": {\"action\": \"checkedout\"}}}"
  })
  void parseLoanEventShouldReturnNullWhenRequiredDataMissing(String payload) {
    // TestMate-f3e1546c9dcc4d07caefcc32b5fb8da0
    var result = TransactionHelper.parseLoanEvent(payload);
    assertThat(result).isNull();
  }

  @Test
  void kafkaEvent_createdTypeWithNewNode_shouldPopulateEventTypeAndNewNode() {
    var kafkaEvent = new KafkaEvent("""
      {
        "type": "CREATED",
        "data": {
          "new": { "id": "1" }
        }
      }
      """);

    assertThat(kafkaEvent.getEventType()).isEqualTo(KafkaEvent.EventType.CREATED);
    assertThat(kafkaEvent.hasNewNode()).isTrue();
    assertThat(kafkaEvent.getNewNode()).isNotNull();
    assertThat(kafkaEvent.getNewNode().get("id").asString()).isEqualTo("1");
  }

  @Test
  void kafkaEvent_updatedTypeWithBothNodes_shouldPopulateEventTypeAndBothNodes() {
    var kafkaEvent = new KafkaEvent("""
      {
        "type": "UPDATED",
        "data": {
          "new": { "id": "2" },
          "old": { "id": "1" }
        }
      }
      """);

    assertThat(kafkaEvent.getEventType()).isEqualTo(KafkaEvent.EventType.UPDATED);
    assertThat(kafkaEvent.hasNewNode()).isTrue();
    assertThat(kafkaEvent.getNewNode()).isNotNull();
    assertThat(kafkaEvent.getNewNode().get("id").asString()).isEqualTo("2");
    assertThat(kafkaEvent.getOldNode()).isNotNull();
    assertThat(kafkaEvent.getOldNode().get("id").asString()).isEqualTo("1");
  }

  @Test
  void kafkaEvent_createdTypeWithoutNodeData_shouldPopulateEventTypeButNotNodes() {
    var kafkaEvent = new KafkaEvent("""
      {
        "type": "CREATED",
        "data": {}
      }
      """);

    assertThat(kafkaEvent.getEventType()).isEqualTo(KafkaEvent.EventType.CREATED);
    assertThat(kafkaEvent.hasNewNode()).isFalse();
    assertThat(kafkaEvent.getNewNode()).isNull();
    assertThat(kafkaEvent.getOldNode()).isNull();
  }

  @Test
  void kafkaEvent_updatedTypeWithoutDataField_shouldPopulateEventTypeButNotNodes() {
    var kafkaEvent = new KafkaEvent("""
      {
        "type": "UPDATED"
      }
      """);

    assertThat(kafkaEvent.getEventType()).isEqualTo(KafkaEvent.EventType.UPDATED);
    assertThat(kafkaEvent.hasNewNode()).isFalse();
    assertThat(kafkaEvent.getNewNode()).isNull();
    assertThat(kafkaEvent.getOldNode()).isNull();
  }

  @Test
  void parseLoanEventShouldReturnNullOnMalformedJson() {
    // TestMate-06cc4a8754ad50afe2bc1c8b65223b28
    var malformedPayload = "{ invalid: json }";
    var result = TransactionHelper.parseLoanEvent(malformedPayload);
    assertThat(result).isNull();
  }

  @ParameterizedTest
  @MethodSource("invalidPayloadProvider")
  void testConstructorWhenPayloadIsInvalidShouldHandleExceptionsGracefully(String invalidPayload) {
    // TestMate-033e475dee0e95c33102bf1a0c69e3a6
    var kafkaEvent = new KafkaEvent(invalidPayload);

    assertThat(kafkaEvent.getEventType()).isNull();
    assertThat(kafkaEvent.getNewNode()).isNull();
    assertThat(kafkaEvent.getOldNode()).isNull();
    assertThat(kafkaEvent.hasNewNode()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"type\":\"UPDATED\", \"data\": {\"old\": {\"id\":\"1\"}}}",
    "{\"type\":\"CREATED\"}",
    "{\"type\":\"UPDATED\", \"data\": {}}"
  })
  void hasNewNodeShouldReturnFalseWhenNewNodeIsMissing(String payload) {
    // TestMate-18a2a23e9dfa165977b4e0a508955ec4
    var kafkaEvent = new KafkaEvent(payload);
    var result = kafkaEvent.hasNewNode();
    assertThat(result).isFalse();
    assertThat(kafkaEvent.getNewNode()).isNull();
  }

  private static Stream<Arguments> invalidPayloadProvider() {
    return Stream.of(
      arguments("{\"type\":\"DELETED\"}"),
      arguments("{\"data\":{}}"),
      arguments("{invalid-json}"),
      arguments((String) null),
      arguments("")
    );
  }

  public static Stream<Arguments> dcbFlagEventPayloadDataSource() {
    var itemId = UUID.randomUUID().toString();
    return Stream.of(
      arguments("dcb flag is provided as true",
        eventString(toJsonNode(Map.of("itemId", itemId, "isDcb", true))), true),
      arguments("dcb flag is provided as false",
        eventString(toJsonNode(Map.of("itemId", itemId, "isDcb", false))), false),
      arguments("dcb flag is provided as null",
        eventString(toJsonNode(Map.of("itemId", itemId))), true)
    );
  }

  public static String eventString(JsonNode newData) {
    var objectNode = JSON_MAPPER.createObjectNode();
    return objectNode
      .put("type", "UPDATED")
      .set("data", JSON_MAPPER.createObjectNode()
        .set("new", newData))
      .toString();
  }
}
