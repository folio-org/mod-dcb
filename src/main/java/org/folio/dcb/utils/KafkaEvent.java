package org.folio.dcb.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
public class KafkaEvent {

  private EventType eventType;
  private JsonNode newNode;
  private JsonNode oldNode;
  private static final ObjectMapper objectMapper = new ObjectMapper();
  public static final String ACTION = "action";
  public static final String STATUS = "status";
  public static final String STATUS_NAME = "name";

  public KafkaEvent(String eventPayload) {
    try {
      JsonNode jsonNode = objectMapper.readTree(eventPayload);
      setEventType(jsonNode.get("type").asText());
      setNewNode(jsonNode.get("data"));
      setOldNode(jsonNode.get("data"));
    } catch (Exception e) {
      log.error("Could not parse input payload for processing event", e);
    }
  }

  private void setEventType(String eventType) {
    this.eventType = EventType.valueOf(eventType);
  }

  private void setNewNode(JsonNode dataNode) {
    if(dataNode != null) {
      this.newNode = dataNode.get("new");
    }
  }

  private void setOldNode(JsonNode dataNode) {
    if(dataNode != null) {
      this.oldNode = dataNode.get("old");
    }
  }

  public boolean hasNewNode() {
    return this.newNode != null;
  }

  enum EventType {
    UPDATED, CREATED
  }
}
