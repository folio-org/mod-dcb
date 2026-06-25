package org.folio.dcb.integration.kafka;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.folio.dcb.integration.kafka.model.KafkaEvent.ACTION;
import static org.folio.dcb.integration.kafka.model.KafkaEvent.STATUS;
import static org.folio.dcb.integration.kafka.model.KafkaEvent.STATUS_NAME;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.integration.circulation.model.RequestStatus;
import org.folio.dcb.integration.kafka.model.EventData;
import org.folio.dcb.integration.kafka.model.KafkaEvent;
import org.springframework.messaging.MessageHeaders;

@Log4j2
public final class TransactionHelper {

  private static final String LOAN_ACTION_CHECKED_OUT = "checkedout";
  private static final String LOAN_ACTION_CHECKED_IN = "checkedin";
  private static final String LOAN_ACTION_CHECKED_IN_FOUND_BY_LIBRARY = "checkedInFoundByLibrary";
  private static final String LOAN_ACTION_CHECKED_IN_RETURNED_BY_PATRON = "checkedInReturnedByPatron";
  private static final String CLAIMED_RETURNED_RESOLUTION_FOUND_BY_LIBRARY = "Found by library";
  private static final String CLAIMED_RETURNED_RESOLUTION_RETURNED_BY_PATRON = "Returned by patron";
  private static final String IS_DCB = "isDcb";
  private static final String INSTANCE = "instance";
  private static final String TITLE = "title";
  private static final String DCB_INSTANCE_TITLE = "DCB_INSTANCE";

  private TransactionHelper() {}

  public static List<String> getHeaderValue(MessageHeaders headers, String headerName, String defaultValue) {
    var headerValue = headers.get(headerName);
    var value = headerValue == null ? defaultValue : new String((byte[]) headerValue, StandardCharsets.UTF_8);
    return value == null ? Collections.emptyList() : Collections.singletonList(value);
  }

  public static EventData parseLoanEvent(String eventPayload) {
    KafkaEvent event = new KafkaEvent(eventPayload);
    if (event.hasNewNode() && event.getNewNode().has("itemId")) {
      var eventData = new EventData();
      eventData.setItemId(event.getNewNode().get("itemId").asString());
      if (event.getNewNode().has(ACTION)) {
        var action = event.getNewNode().get(ACTION).asString();
        if (LOAN_ACTION_CHECKED_OUT.equals(action)) {
          eventData.setType(EventData.EventType.CHECK_OUT);
        } else if (LOAN_ACTION_CHECKED_IN.equals(action)) {
          eventData.setType(EventData.EventType.CHECK_IN);
        } else if (LOAN_ACTION_CHECKED_IN_FOUND_BY_LIBRARY.equals(action)) {
          eventData.setType(EventData.EventType.CHECK_IN);
          eventData.setClaimedReturnedResolution(CLAIMED_RETURNED_RESOLUTION_FOUND_BY_LIBRARY);
        } else if (LOAN_ACTION_CHECKED_IN_RETURNED_BY_PATRON.equals(action)) {
          eventData.setType(EventData.EventType.CHECK_IN);
          eventData.setClaimedReturnedResolution(CLAIMED_RETURNED_RESOLUTION_RETURNED_BY_PATRON);
        }
      }
      eventData.setDcb(!event.getNewNode().has(IS_DCB) || event.getNewNode().get(IS_DCB).asBoolean());

      if (event.getNewNode().has(STATUS) && event.getNewNode().get(STATUS).has(STATUS_NAME)) {
        eventData.setLoanStatus(event.getNewNode().get(STATUS).get(STATUS_NAME).asString());
      }

      return eventData;
    }
    return null;
  }

  public static EventData parseRequestEvent(String eventPayload) {
    var event = new KafkaEvent(eventPayload);
    if (event.getEventType() == KafkaEvent.EventType.UPDATED && event.hasNewNode() && event.getNewNode().has(STATUS)) {
      var eventData = new EventData();
      eventData.setRequestId(event.getNewNode().get("id").asString());
      eventData.setDcbReRequestCancellation(getNodeAsBooleanOrDefault(event, "dcbReRequestCancellation", false));

      var requestStatus = RequestStatus.from(event.getNewNode().get(STATUS).asString());
      switch (requestStatus) {
        case OPEN_IN_TRANSIT -> eventData.setType(EventData.EventType.IN_TRANSIT);
        case OPEN_AWAITING_PICKUP, OPEN_AWAITING_DELIVERY -> eventData.setType(EventData.EventType.AWAITING_PICKUP);
        case CLOSED_CANCELLED, CLOSED_UNFILLED -> eventData.setType(EventData.EventType.CANCEL);
        case CLOSED_PICKUP_EXPIRED -> eventData.setType(EventData.EventType.EXPIRED);
        default -> log.info("parseRequestEvent:: Request status {} is not supported", requestStatus);
      }

      eventData.setDcb(checkDcbRequest(event));
      return eventData;
    }
    return null;
  }

  public static EventData parseCheckInEvent(String eventPayload) {
    var kafkaEvent = new KafkaEvent(eventPayload);
    if (kafkaEvent.getEventType() == KafkaEvent.EventType.CREATED && kafkaEvent.hasNewNode()) {
      var eventData = new EventData();
      var newNode = kafkaEvent.getNewNode();
      eventData.setItemId(trimToNull(newNode.path("itemId").asString(null)));
      eventData.setCheckInServicePointId(trimToNull(newNode.path("servicePointId").asString(null)));
      return eventData;
    }

    return null;
  }

  @SuppressWarnings("SameParameterValue")
  private static boolean getNodeAsBooleanOrDefault(KafkaEvent kafkaEvent, String name, boolean defaultValue) {
    var booleanNode = kafkaEvent.getNewNode().get(name);
    return Objects.nonNull(booleanNode) ? booleanNode.asBoolean() : defaultValue;
  }

  private static boolean checkDcbRequest(KafkaEvent kafkaEvent) {
    var newNode = kafkaEvent.getNewNode();
    return newNode.has(INSTANCE)
      && newNode.get(INSTANCE).has(TITLE)
      && Objects.equals(DCB_INSTANCE_TITLE, newNode.get(INSTANCE).get(TITLE).asString());
  }
}
