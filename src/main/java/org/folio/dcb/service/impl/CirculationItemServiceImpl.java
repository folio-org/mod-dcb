package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dcb.client.feign.CirculationItemClient;
import static org.folio.dcb.client.feign.LocationsClient.LocationDTO;

import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.ItemStatus;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.HoldingsService;
import org.folio.dcb.service.ItemService;
import org.folio.spring.model.ResultList;
import org.folio.util.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

import static org.folio.dcb.domain.dto.ItemStatus.NameEnum.IN_TRANSIT;
import static org.folio.dcb.utils.DCBConstants.CODE;
import static org.folio.dcb.utils.DCBConstants.LOAN_TYPE_ID;
import static org.folio.dcb.utils.DCBConstants.LOCATION_ID;
import static org.folio.dcb.utils.DCBConstants.MATERIAL_TYPE_NAME_BOOK;
import static org.folio.dcb.utils.DCBConstants.NAME;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationItemServiceImpl implements CirculationItemService {

  private final ItemService itemService;
  private final CirculationItemClient circulationItemClient;
  private final LocationsClient locationsClient;
  private final HoldingsService holdingsService;


  @Value("${application.dcb-hub.fetch-dcb-locations-enabled}")
  private Boolean isFetchDcbHubLocationsEnabled;


  @Override
  public CirculationItem checkIfItemExistsAndCreate(DcbItem dcbItem, String pickupServicePointId, String locationCode) {
    var dcbItemBarcode = dcbItem.getBarcode();
    log.debug("checkIfItemExistsAndCreate:: generate Circulation item with barcode {} if it doesn't exist.", dcbItemBarcode);
    var circulationItem = fetchCirculationItemByBarcode(dcbItem.getBarcode());
    if(Objects.isNull(circulationItem)) {
      log.warn("checkIfItemExistsAndCreate:: Circulation item not found by barcode={}. Creating it.", dcbItemBarcode);
      String effectiveLocationId = fetchShadowLocationIdByLocationCode(locationCode);
      circulationItem = createCirculationItem(dcbItem, pickupServicePointId, effectiveLocationId);
    }
    return circulationItem;
  }

  private String fetchShadowLocationIdByLocationCode(String locationCode) {
    log.debug(
      "fetchShadowLocationIdByLocationCode:: Fetching shadow location id by location code: {} and isFetchDcbHubLocationsEnabled: {}",
      locationCode, isFetchDcbHubLocationsEnabled);
    if(Boolean.TRUE.equals(isFetchDcbHubLocationsEnabled) && StringUtils.isNotBlank(locationCode)) {
      ResultList<LocationDTO> locationDTOResult = locationsClient.findLocationByQuery(String.format("code==%s", locationCode), true, 1, 0);
      if(locationDTOResult.getResult().isEmpty()) {
        log.warn(
          "fetchShadowLocationIdByLocationCode:: No shadow location found for code: {}. Falling back to default location id: {}, code: {}, name: {}",
          locationCode, LOCATION_ID, CODE, NAME);
        return LOCATION_ID;
      }

      LocationDTO locationDTO = locationDTOResult.getResult().getFirst();
      log.debug("fetchShadowLocationIdByLocationCode:: Shadow location lookup is enabled. Found location for code: {} with id: {}",
        locationCode, locationDTO.getId());
      return locationDTO.getId();
    } else {
      log.warn(
        "fetchShadowLocationIdByLocationCode:: Shadow location lookup is disabled or location code is blank. " +
        "Falling back to default location id: {}, code: {}, name: {}",
        LOCATION_ID, CODE, NAME);
      return LOCATION_ID;
    }
  }

  private CirculationItem fetchCirculationItemByBarcode(String barcode) {
    return circulationItemClient.fetchItemByCqlQuery("barcode==" + StringUtil.cqlEncode(barcode))
      .getItems()
      .stream()
      .findFirst()
      .orElse(null);
  }

  public CirculationItem fetchItemById(String itemId) {
    log.info("fetchItemById:: fetching item details for id {} ", itemId);
    return circulationItemClient.retrieveCirculationItemById(itemId);
  }

  private CirculationItem createCirculationItem(DcbItem item, String pickupServicePointId, String effectiveLocationId){
    //SetupDefaultMaterialTypeIfNotGiven
    String materialType = StringUtils.isBlank(item.getMaterialType()) ? MATERIAL_TYPE_NAME_BOOK : item.getMaterialType();
    var materialTypeId = itemService.fetchItemMaterialTypeIdByMaterialTypeName(materialType);
    var dcbHolding = holdingsService.fetchDcbHoldingOrCreateIfMissing();
    var itemId = UUID.randomUUID().toString();
    CirculationItem circulationItem =
      CirculationItem.builder()
        .id(itemId)
        .barcode(item.getBarcode())
        .status(ItemStatus.builder()
          .name(IN_TRANSIT)
          .build())
        .holdingsRecordId(dcbHolding.getId())
        .instanceTitle(item.getTitle())
        .materialTypeId(materialTypeId)
        .permanentLoanTypeId(LOAN_TYPE_ID)
        .pickupLocation(pickupServicePointId)
        .lendingLibraryCode(item.getLendingLibraryCode())
        .effectiveLocationId(effectiveLocationId)
        .build();

    return circulationItemClient.createCirculationItem(itemId, circulationItem);
  }
}
