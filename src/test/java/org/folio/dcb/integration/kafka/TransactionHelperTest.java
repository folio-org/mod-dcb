package org.folio.dcb.integration.kafka;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.folio.dcb.integration.kafka.TransactionHelper;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class TransactionHelperTest {

    @Test
  void getHeaderValue_positive_shouldReturnHeaderValueWhenExists() {
    // TestMate-2d5040ffc335eec4a8a7920946d099b8
    // Given
    String headerName = "x-okapi-tenant";
    String headerValue = "diku";
    String defaultValue = "default-tenant";
    byte[] headerValueBytes = headerValue.getBytes(StandardCharsets.UTF_8);
    Map<String, Object> headersMap = new HashMap<>();
    headersMap.put(headerName, headerValueBytes);
    MessageHeaders headers = new MessageHeaders(headersMap);
    // When
    List<String> result = TransactionHelper.getHeaderValue(headers, headerName, defaultValue);
    // Then
    assertThat(result)
      .hasSize(1)
      .containsExactly(headerValue);
  }

    @Test
  void getHeaderValue_positive_shouldReturnDefaultValueWhenHeaderMissing() {
    // TestMate-0d2c51271449a74c5b6391cb2241f2a8
    // Given
    String headerName = "missing-header";
    String defaultValue = "fallback";
    Map<String, Object> headersMap = new HashMap<>();
    MessageHeaders headers = new MessageHeaders(headersMap);
    // When
    List<String> result = TransactionHelper.getHeaderValue(headers, headerName, defaultValue);
    // Then
    assertThat(result)
      .hasSize(1)
      .containsExactly(defaultValue);
  }

    @Test
  void getHeaderValue_positive_shouldReturnEmptyListWhenBothValueAndDefaultAreNull() {
    // TestMate-4423ae29a46220bb8c5e8ca0ce09bb97
    // Given
    String headerName = "non-existent-header";
    String defaultValue = null;
    Map<String, Object> headersMap = new HashMap<>();
    MessageHeaders headers = new MessageHeaders(headersMap);
    // When
    List<String> result = TransactionHelper.getHeaderValue(headers, headerName, defaultValue);
    // Then
    assertThat(result).isEmpty();
  }

    @Test
  void getHeaderValue_positive_shouldHandleEmptyByteArrayHeader() {
    // TestMate-bfbbbd329f6a2dc1d12c169998db4db5
    // Given
    String headerName = "test-header";
    String defaultValue = "default";
    byte[] emptyHeaderValue = new byte[0];
    Map<String, Object> headersMap = new HashMap<>();
    headersMap.put(headerName, emptyHeaderValue);
    MessageHeaders headers = new MessageHeaders(headersMap);
    // When
    List<String> result = TransactionHelper.getHeaderValue(headers, headerName, defaultValue);
    // Then
    assertThat(result)
      .hasSize(1)
      .containsExactly("");
  }

    @Test
  void getHeaderValue_positive_shouldHandleSpecialCharactersInUtf8() {
    // TestMate-75141cc3fdff80ada4b6ce4d76e31673
    // Given
    String headerName = "special-header";
    String headerValue = "tést-vålue";
    String defaultValue = "default";
    byte[] headerValueBytes = headerValue.getBytes(StandardCharsets.UTF_8);
    Map<String, Object> headersMap = new HashMap<>();
    headersMap.put(headerName, headerValueBytes);
    MessageHeaders headers = new MessageHeaders(headersMap);
    // When
    List<String> result = TransactionHelper.getHeaderValue(headers, headerName, defaultValue);
    // Then
    assertThat(result)
      .hasSize(1)
      .containsExactly(headerValue);
  }

}
