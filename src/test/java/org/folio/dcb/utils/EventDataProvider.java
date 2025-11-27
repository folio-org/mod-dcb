package org.folio.dcb.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.folio.dcb.utils.EntityUtils.ITEM_ID;
import static org.folio.dcb.utils.EntityUtils.PICKUP_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.REQUEST_ID;
import static org.folio.dcb.utils.EntityUtils.TEST_TENANT;
import static org.folio.dcb.utils.JsonTestUtils.objectMapper;
import static org.folio.dcb.utils.JsonTestUtils.toJsonNode;
import static org.folio.dcb.utils.RequestStatus.CLOSED_PICKUP_EXPIRED;
import static org.folio.dcb.utils.RequestStatus.OPEN_AWAITING_PICKUP;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.spring.integration.XOkapiHeaders;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventDataProvider {

  public static ProducerRecord<String, Object> expiredRequestMessage(String tenantId) {
    var topic = circulationRequestsTopic(tenantId);
    var record = new ProducerRecord<String, Object>(topic, REQUEST_ID, expiredRequestPayload());
    record.headers().add(XOkapiHeaders.TENANT, tenantId.getBytes(UTF_8));
    return record;
  }

  public static ProducerRecord<String, Object> itemCheckInMessage(String tenantId) {
    var topic = circulationCheckInTopic(tenantId);
    var record = new ProducerRecord<String, Object>(topic, ITEM_ID, itemCheckInPayload());
    record.headers().add(XOkapiHeaders.TENANT, tenantId.getBytes(UTF_8));
    return record;
  }

  private static JsonNode expiredRequestPayload() {
    var objectNode = objectMapper.createObjectNode();
    objectNode.put("id", REQUEST_ID);
    objectNode.put("type", "UPDATED");
    objectNode.put("tenant", TEST_TENANT);
    objectNode.put("timestamp", System.currentTimeMillis());

    var dataNode = objectMapper.createObjectNode();
    dataNode.set("new", toJsonNode(circulationRequest(CLOSED_PICKUP_EXPIRED)));
    dataNode.set("old", toJsonNode(circulationRequest(OPEN_AWAITING_PICKUP)));

    objectNode.set("data", dataNode);
    return objectNode;
  }

  private static JsonNode itemCheckInPayload() {
    var objectNode = objectMapper.createObjectNode();
    objectNode.put("id", UUID.randomUUID().toString());
    objectNode.put("type", "CREATED");
    objectNode.put("tenant", TEST_TENANT);
    objectNode.put("timestamp", System.currentTimeMillis());

    var dataNode = objectMapper.createObjectNode();
    dataNode.set("new", toJsonNode(circulationRequest(CLOSED_PICKUP_EXPIRED)));

    objectNode.set("data", dataNode);
    return objectNode;
  }

  private static CirculationRequest circulationRequest(RequestStatus status) {
    return new CirculationRequest()
      .id(REQUEST_ID)
      .requestType(CirculationRequest.RequestTypeEnum.PAGE)
      .itemId(UUID.fromString(ITEM_ID))
      .status(status.getValue());
  }

  private static JsonNode checkInRequest() {
    var checkInBody = objectMapper.createObjectNode();
    checkInBody.put("id", UUID.randomUUID().toString());
    checkInBody.put("occurredDateTime", OffsetDateTime.now().toString());
    checkInBody.put("itemId", ITEM_ID);
    checkInBody.put("itemStatusPriorToCheckIn", OPEN_AWAITING_PICKUP.getValue());
    checkInBody.put("requestQueueSize", 0);
    checkInBody.put("itemLocationId", UUID.randomUUID().toString());
    checkInBody.put("servicePointId", PICKUP_SERVICE_POINT_ID);
    checkInBody.put("973fcb0e-2b4a-4c60-bc8c-8d74de7bc1c6", PICKUP_SERVICE_POINT_ID);

    return checkInBody;
  }

  public static String circulationRequestsTopic(String tenantId) {
    return "folio.%s.circulation.request".formatted(tenantId);
  }

  private static String circulationCheckInTopic(String tenantId) {
    return "folio.%s.circulation.check-in".formatted(tenantId);
  }
}
