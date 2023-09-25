package org.folio.dcb.utils;

import org.springframework.messaging.MessageHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class TransactionHelper {

  private TransactionHelper(){}

  public static List<String> getHeaderValue(MessageHeaders headers, String headerName, String defaultValue) {
    var headerValue = headers.get(headerName);
    var value = headerValue == null
      ? defaultValue
      : new String((byte[]) headerValue, StandardCharsets.UTF_8);
    return value == null ? Collections.emptyList() : Collections.singletonList(value);
  }
}
