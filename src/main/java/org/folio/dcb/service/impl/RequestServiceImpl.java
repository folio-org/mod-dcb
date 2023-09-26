package org.folio.dcb.service.impl;

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
  public void createPageItemRequest(User user, DcbItem item) {
    log.debug("createPageItemRequest:: creating a new page request for userBarcode {} , itemBarcode {}",
      user.getBarcode(), item.getBarcode());
    var inventoryItem = itemService.fetchItemDetailsById(item.getId());
    var inventoryHolding = holdingsService.fetchInventoryHoldingDetails(inventoryItem.getHoldingsRecordId());
    var circulationRequest = createCirculationRequest(user, item, inventoryItem.getHoldingsRecordId(), inventoryHolding.getInstanceId());
    circulationClient.createRequest(circulationRequest);
  }

  private CirculationRequest createCirculationRequest(User user, DcbItem item, String holdingsId, String instanceId) {
    return CirculationRequest.builder()
      .id(UUID.randomUUID().toString())
      .requesterId(UUID.fromString(user.getId()))
      .itemId(UUID.fromString(item.getId()))
      .requestDate(OffsetDateTime.now().toString())
      .instanceId(UUID.fromString(instanceId))
      .holdingsRecordId(UUID.fromString(holdingsId))
      .requestType(CirculationRequest.RequestTypeEnum.PAGE)
      .requestLevel(CirculationRequest.RequestLevelEnum.ITEM)
      .fulfillmentPreference(CirculationRequest.FulfillmentPreferenceEnum.HOLD_SHELF)
      .requester(Requester.builder().barcode(user.getBarcode()).build())
      .item(Item.builder().barcode(item.getBarcode()).build())
      //As we don't know the servicePoint logic yet, proceeding with hardcoded value
      .pickupServicePointId("3a40852d-49fd-4df2-a1f9-6e2641a6e91f")
      .build();
  }

}
