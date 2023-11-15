package org.folio.dcb.service.impl;

import static org.folio.dcb.domain.dto.CirculationRequest.RequestTypeEnum.PAGE;
import static org.folio.dcb.domain.dto.CirculationRequest.RequestTypeEnum.HOLD;
import static org.folio.dcb.utils.DCBConstants.HOLDING_ID;
import static org.folio.dcb.utils.DCBConstants.INSTANCE_ID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.Item;
import org.folio.dcb.domain.dto.Requester;
import org.folio.dcb.domain.dto.User;
import org.folio.dcb.service.HoldingsService;
import org.folio.dcb.service.ItemService;
import org.folio.dcb.service.RequestService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

  private final ItemService itemService;
  private final HoldingsService holdingsService;
  private final CirculationClient circulationClient;

  @Override
  public void createPageItemRequest(User user, DcbItem item, String pickupServicePointId) {
    log.debug("createPageItemRequest:: creating a new page request for userBarcode {} , itemBarcode {}",
      user.getBarcode(), item.getBarcode());
    var inventoryItem = itemService.fetchItemDetailsById(item.getId());
    var inventoryHolding = holdingsService.fetchInventoryHoldingDetailsByHoldingId(inventoryItem.getHoldingsRecordId());
    var circulationRequest = createCirculationRequest(PAGE, user, item, inventoryItem.getHoldingsRecordId(), inventoryHolding.getInstanceId(), pickupServicePointId);
    circulationClient.createRequest(circulationRequest);
  }

  @Override
  public void createHoldItemRequest(User user, DcbItem item, String pickupServicePointId) {
    log.debug("createHoldItemRequest:: creating a new hold request for userBarcode {} , itemBarcode {}",
      user.getBarcode(), item.getBarcode());
    var circulationRequest = createCirculationRequest(HOLD, user, item, HOLDING_ID, INSTANCE_ID, pickupServicePointId);
    circulationClient.createRequest(circulationRequest);
  }

  private CirculationRequest createCirculationRequest(CirculationRequest.RequestTypeEnum type, User user, DcbItem item, String holdingsId, String instanceId, String pickupServicePointId) {
    return CirculationRequest.builder()
      .id(UUID.randomUUID().toString())
      .requesterId(UUID.fromString(user.getId()))
      .itemId(UUID.fromString(item.getId()))
      .requestDate(OffsetDateTime.now().toString())
      .instanceId(UUID.fromString(instanceId))
      .holdingsRecordId(UUID.fromString(holdingsId))
      .requestType(type)
      .requestLevel(CirculationRequest.RequestLevelEnum.ITEM)
      .fulfillmentPreference(CirculationRequest.FulfillmentPreferenceEnum.HOLD_SHELF)
      .requester(Requester.builder().barcode(user.getBarcode()).personal(user.getPersonal()).build())
      .item(Item.builder().barcode(item.getBarcode()).build())
      .pickupServicePointId(pickupServicePointId)
      .build();
  }

}
