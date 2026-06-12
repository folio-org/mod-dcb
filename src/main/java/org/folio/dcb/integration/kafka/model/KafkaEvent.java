package org.folio.dcb.integration.kafka.model;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@Getter
public class KafkaEvent {

  public static final String ACTION = "action";
  public static final String STATUS = "status";
  public static final String STATUS_NAME = "name";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private EventType eventType;
  private JsonNode newNode;
  private JsonNode oldNode;

  public KafkaEvent(String eventPayload) {
    try {
      JsonNode jsonNode = OBJECT_MAPPER.readTree(eventPayload);
      setEventType(jsonNode.get("type").asString());
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
    if (dataNode != null) {
      this.newNode = dataNode.get("new");
    }
  }

  private void setOldNode(JsonNode dataNode) {
    if (dataNode != null) {
      this.oldNode = dataNode.get("old");
    }
  }

  public boolean hasNewNode() {
    return this.newNode != null;
  }

  public enum EventType {
    UPDATED, CREATED
  }
}
