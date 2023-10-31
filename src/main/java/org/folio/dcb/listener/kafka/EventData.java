package org.folio.dcb.listener.kafka;

import lombok.Data;

@Data
public class EventData {
  private EventType type;
  private String itemId;

  public enum EventType {
    CHECK_IN, CHECK_OUT
  }
}
