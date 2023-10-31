package org.folio.dcb.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.listener.kafka.EventData;
import org.springframework.messaging.MessageHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Log4j2
public class TransactionHelper {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private TransactionHelper(){}

  public static List<String> getHeaderValue(MessageHeaders headers, String headerName, String defaultValue) {
    var headerValue = headers.get(headerName);
    var value = headerValue == null
      ? defaultValue
      : new String((byte[]) headerValue, StandardCharsets.UTF_8);
    return value == null ? Collections.emptyList() : Collections.singletonList(value);
  }

  public static EventData parseEvent(String eventPayload) {
    try {
      JsonNode jsonNode = objectMapper.readTree(eventPayload);
      JsonNode dataNode = jsonNode.get("data");
      String typeNode = jsonNode.get("type").asText();
      JsonNode newDataNode = (dataNode != null) ? dataNode.get("new") : null;

      if (newDataNode != null && newDataNode.has("itemId") && typeNode.equals("CREATED")) {
        EventData eventData = new EventData();
        eventData.setItemId(newDataNode.get("itemId").asText());

        if (newDataNode.has("action")) {
          eventData.setType(EventData.EventType.CHECK_OUT);
        } else {
          eventData.setType(EventData.EventType.CHECK_IN);
        }

        return eventData;
      }
    } catch (Exception e) {
      log.error("Could not parse input payload for processing event", e);
    }
    return null;
  }
}
