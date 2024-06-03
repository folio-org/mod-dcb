package org.folio.dcb.utils;

import lombok.extern.log4j.Log4j2;
import org.folio.dcb.listener.kafka.EventData;
import org.springframework.messaging.MessageHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.folio.dcb.utils.KafkaEvent.ACTION;
import static org.folio.dcb.utils.KafkaEvent.STATUS;

@Log4j2
public class TransactionHelper {
  private static final String LOAN_ACTION_CHECKED_OUT = "checkedout";
  private static final String LOAN_ACTION_CHECKED_IN = "checkedin";
  public static final String IS_DCB = "isDcb";
  public static final String INSTANCE = "instance";
  public static final String REQUESTER = "requester";
  public static final String TITLE = "title";
  public static final String LASTNAME = "lastName";
  public static final String DCB_INSTANCE_TITLE = "DCB_INSTANCE";
  public static final String DCB_REQUESTER_LASTNAME = "DcbSystem";

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
        eventData.setDcb(kafkaEvent.getNewNode().has(IS_DCB) && kafkaEvent.getNewNode().get(IS_DCB).asBoolean());
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
        RequestStatus requestStatus = RequestStatus.from(kafkaEvent.getNewNode().get(STATUS).asText());
        switch (requestStatus) {
          case OPEN_IN_TRANSIT -> eventData.setType(EventData.EventType.IN_TRANSIT);
          case OPEN_AWAITING_PICKUP -> eventData.setType(EventData.EventType.AWAITING_PICKUP);
          case CLOSED_CANCELLED -> eventData.setType(EventData.EventType.CANCEL);
          default -> log.info("parseRequestEvent:: Request status {} is not supported", requestStatus);
        }
        eventData.setDcb(checkDcbRequest(kafkaEvent));
        return eventData;
      }
    return null;
  }
  private static boolean checkDcbRequest(KafkaEvent kafkaEvent) {
    return (kafkaEvent.getNewNode().has(INSTANCE) && kafkaEvent.getNewNode().get(INSTANCE).has(TITLE)
      && kafkaEvent.getNewNode().get(INSTANCE).get(TITLE).asText().equals(DCB_INSTANCE_TITLE)) || (kafkaEvent.getNewNode().has(REQUESTER)
      && kafkaEvent.getNewNode().get(REQUESTER).has(LASTNAME) && kafkaEvent.getNewNode().get(REQUESTER).get(LASTNAME).asText().equals(DCB_REQUESTER_LASTNAME));
  }
}
