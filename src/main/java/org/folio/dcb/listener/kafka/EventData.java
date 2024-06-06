package org.folio.dcb.listener.kafka;

import lombok.Data;

@Data
public class EventData {
  private EventType type;
  private String itemId;
  private String requestId;
  private boolean isDcb;

  public enum EventType {
    CHECK_IN, CHECK_OUT, IN_TRANSIT, AWAITING_PICKUP, CANCEL
  }
}
