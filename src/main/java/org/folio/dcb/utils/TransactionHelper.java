package org.folio.dcb.utils;

import lombok.extern.log4j.Log4j2;
import org.folio.dcb.listener.kafka.EventData;
import org.springframework.messaging.MessageHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.folio.dcb.utils.KafkaEvent.ACTION;
import static org.folio.dcb.utils.KafkaEvent.STATUS;
import static org.folio.dcb.utils.KafkaEvent.STATUS_NAME;

import com.fasterxml.jackson.databind.JsonNode;

@Log4j2
public class TransactionHelper {
  private static final String LOAN_ACTION_CHECKED_OUT = "checkedout";
  private static final String LOAN_ACTION_CHECKED_IN = "checkedin";
  public static final String IS_DCB = "isDcb";
  public static final String INSTANCE = "instance";
  public static final String TITLE = "title";
  public static final String DCB_INSTANCE_TITLE = "DCB_INSTANCE";

  private TransactionHelper(){}

  public static List<String> getHeaderValue(MessageHeaders headers, String headerName, String defaultValue) {
    var headerValue = headers.get(headerName);
    var value = headerValue == null
      ? defaultValue
      : new String((byte[]) headerValue, StandardCharsets.UTF_8);
    return value == null ? Collections.emptyList() : Collections.singletonList(value);
  }

  public static EventData parseLoanEvent(String eventPayload) {
      KafkaEvent kafkaEvent = new KafkaEvent(eventPayload);
      if (kafkaEvent.hasNewNode() && kafkaEvent.getNewNode().has("itemId")) {
        EventData eventData = new EventData();
        eventData.setItemId(kafkaEvent.getNewNode().get("itemId").asText());
        if (kafkaEvent.getNewNode().has(ACTION)) {
          if(LOAN_ACTION_CHECKED_OUT.equals(kafkaEvent.getNewNode().get(ACTION).asText())){
            eventData.setType(EventData.EventType.CHECK_OUT);
          } else if(LOAN_ACTION_CHECKED_IN.equals(kafkaEvent.getNewNode().get(ACTION).asText())) {
            eventData.setType(EventData.EventType.CHECK_IN);
          }
        }
        eventData.setDcb(!kafkaEvent.getNewNode().has(IS_DCB) || kafkaEvent.getNewNode().get(IS_DCB).asBoolean());

        if (kafkaEvent.getNewNode().has(STATUS) && kafkaEvent.getNewNode().get(STATUS).has(STATUS_NAME)) {
          eventData.setLoanStatus(kafkaEvent.getNewNode().get(STATUS).get(STATUS_NAME).asText());
        }
        return eventData;
      }
    return null;
  }

  public static EventData parseRequestEvent(String eventPayload){
      KafkaEvent kafkaEvent = new KafkaEvent(eventPayload);
      if(kafkaEvent.getEventType() == KafkaEvent.EventType.UPDATED && kafkaEvent.hasNewNode()
        && kafkaEvent.getNewNode().has(STATUS)){
        EventData eventData = new EventData();
        eventData.setRequestId(kafkaEvent.getNewNode().get("id").asText());
        eventData.setDcbReRequestCancellation(
          getNodeAsBooleanOrDefault(kafkaEvent, "dcbReRequestCancellation", false));
        RequestStatus requestStatus = RequestStatus.from(kafkaEvent.getNewNode().get(STATUS).asText());
        switch (requestStatus) {
          case OPEN_IN_TRANSIT -> eventData.setType(EventData.EventType.IN_TRANSIT);
          case OPEN_AWAITING_PICKUP, OPEN_AWAITING_DELIVERY ->
            eventData.setType(EventData.EventType.AWAITING_PICKUP);
          case CLOSED_CANCELLED, CLOSED_UNFILLED ->
            eventData.setType(EventData.EventType.CANCEL);
          case CLOSED_PICKUP_EXPIRED ->
            eventData.setType(EventData.EventType.EXPIRED);
          default -> log.info("parseRequestEvent:: Request status {} is not supported", requestStatus);
        }
        eventData.setDcb(checkDcbRequest(kafkaEvent));
        return eventData;
      }
    return null;
  }

  public static EventData parseCheckInEvent(String eventPayload) {
    var kafkaEvent = new KafkaEvent(eventPayload);
    if (kafkaEvent.getEventType() == KafkaEvent.EventType.CREATED && kafkaEvent.hasNewNode()) {
      var eventData = new EventData();
      var newNode = kafkaEvent.getNewNode();
      eventData.setItemId(newNode.get("itemId").asText());
      return eventData;
    }

    return null;
  }

  private static boolean getNodeAsBooleanOrDefault(KafkaEvent kafkaEvent, String name,
    boolean defaultValue) {

    JsonNode booleanNode = kafkaEvent.getNewNode().get(name);
    return Objects.nonNull(booleanNode)
      ? booleanNode.asBoolean()
      : defaultValue;
  }

  private static boolean checkDcbRequest(KafkaEvent kafkaEvent) {
    var newNode = kafkaEvent.getNewNode();
    return newNode.has(INSTANCE)
      && newNode.get(INSTANCE).has(TITLE)
      && Objects.equals(DCB_INSTANCE_TITLE, newNode.get(INSTANCE).get(TITLE).asText());
  }
}
