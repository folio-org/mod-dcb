package org.folio.dcb.integration.kafka.model;

import lombok.Data;

@Data
public class EventData {
  private EventType type;
  private String itemId;
  private String requestId;
  private boolean isDcb;
  private boolean isDcbReRequestCancellation;
  private String loanStatus;
  private String checkInServicePointId;

  public enum EventType {
    CHECK_IN, CHECK_OUT, IN_TRANSIT, AWAITING_PICKUP, CANCEL, EXPIRED
  }
}
