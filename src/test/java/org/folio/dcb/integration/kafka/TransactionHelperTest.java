package org.folio.dcb.integration.kafka;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.folio.dcb.integration.kafka.model.EventData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;

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

  @Test
  void parseLoanEventShouldDefaultIsDcbToTrueWhenMissing() {
    var itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    var payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s"
          }
        }
      }
      """.formatted(itemId);

    var result = TransactionHelper.parseLoanEvent(payload);

    assertThat(result).isNotNull()
      .extracting(EventData::isDcb)
      .isEqualTo(true);
  }

  @Test
  void parseLoanEventShouldPreserveIsDcbWhenTrue() {
    var itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    var payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "isDcb": true
          }
        }
      }
      """.formatted(itemId);

    var result = TransactionHelper.parseLoanEvent(payload);

    assertThat(result).isNotNull()
      .extracting(EventData::isDcb)
      .isEqualTo(true);
  }

  @Test
  void parseLoanEventShouldPreserveIsDcbWhenFalse() {
    var itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    var payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "isDcb": false
          }
        }
      }
      """.formatted(itemId);

    var result = TransactionHelper.parseLoanEvent(payload);

    assertThat(result).isNotNull()
      .extracting(EventData::isDcb)
      .isEqualTo(false);
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
}
