package org.folio.dcb.service.impl;

import static org.folio.dcb.client.feign.LocationsClient.LocationDTO;
import static org.folio.dcb.domain.dto.ItemStatus.NameEnum.IN_TRANSIT;
import static org.folio.dcb.utils.DCBConstants.CODE;
import static org.folio.dcb.utils.DCBConstants.MATERIAL_TYPE_NAME_BOOK;
import static org.folio.dcb.utils.DCBConstants.NAME;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dcb.client.feign.CirculationItemClient;
import org.folio.dcb.client.feign.LocationUnitClient;
import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.integration.dcb.config.DcbHubProperties;
import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.ItemStatus;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.ItemService;
import org.folio.dcb.service.entities.DcbEntityServiceFacade;
import org.folio.dcb.utils.CqlQuery;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationItemServiceImpl implements CirculationItemService {

  private final ItemService itemService;
  private final CirculationItemClient circulationItemClient;
  private final LocationsClient locationsClient;
  private final LocationUnitClient locationUnitClient;
  private final DcbHubProperties dcbHubProperties;
  private final DcbEntityServiceFacade dcbEntityService;

  @Override
  public CirculationItem checkIfItemExistsAndCreate(DcbItem dcbItem, String pickupServicePointId) {
    var dcbItemBarcode = dcbItem.getBarcode();
    log.debug("checkIfItemExistsAndCreate:: generate Circulation item with barcode {} if it doesn't exist.", dcbItemBarcode);
    var circulationItem = fetchCirculationItemByBarcode(dcbItem.getBarcode());
    if(Objects.isNull(circulationItem)) {
      log.warn("checkIfItemExistsAndCreate:: Circulation item not found by barcode={}. Creating it.", dcbItemBarcode);
      String effectiveLocationId = fetchShadowLocationForItem(dcbItem);
      circulationItem = createCirculationItem(dcbItem, pickupServicePointId, effectiveLocationId);
    }
    return circulationItem;
  }

  private String fetchShadowLocationForItem(DcbItem dcbItem) {
    if (dcbHubProperties.isFetchDcbLocationsEnabled()) {
      return tryFetchLocationIdByLocationCode(dcbItem)
        .or(() -> tryFetchLocationIdByLendingLibraryCode(dcbItem))
        .orElseGet(this::getDefaultDcbLocationId);
    }

    log.debug("fetchShadowLocationForItem:: Shadow location lookup is disabled");
    return getDefaultDcbLocationId();
  }

  private CirculationItem fetchCirculationItemByBarcode(String barcode) {
    return circulationItemClient.fetchItemByCqlQuery(CqlQuery.exactMatch("barcode", barcode))
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
    var dcbHolding = dcbEntityService.findOrCreateHolding();
    var itemId = UUID.randomUUID().toString();
    var loanType = dcbEntityService.findOrCreateLoanType();
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
        .permanentLoanTypeId(loanType.getId())
        .pickupLocation(pickupServicePointId)
        .lendingLibraryCode(item.getLendingLibraryCode())
        .effectiveLocationId(effectiveLocationId)
        .build();

    return circulationItemClient.createCirculationItem(itemId, circulationItem);
  }

  private Optional<String> tryFetchLocationIdByLocationCode(DcbItem dcbItem) {
    var locationCode = dcbItem.getLocationCode();
    if (StringUtils.isBlank(locationCode)) {
      log.debug("tryFetchLocationIdByLocationCode:: Location code is blank, shadow location fetch skipped.");
      return Optional.empty();
    }

    log.debug("tryFetchLocationIdByLocationCode:: Fetching shadow location id by location code: {}", locationCode);
    var query = CqlQuery.exactMatch("code", locationCode);
    var locationDTOResult = locationsClient.findLocationByQuery(query, true, 1, 0);
    if(locationDTOResult.getResult().isEmpty()) {
      log.debug("tryFetchLocationIdByLocationCode:: No shadow location found for code: {}.", locationCode);
      return Optional.empty();
    }

    var locationDTO = locationDTOResult.getResult().getFirst();
    log.debug("tryFetchLocationIdByLocationCode:: "
        + "Shadow location lookup is enabled. Found location for code: {} with id: {}",
      locationCode, locationDTO.getId());
    return Optional.ofNullable(locationDTO.getId());
  }

  private Optional<String> tryFetchLocationIdByLendingLibraryCode(DcbItem dcbItem) {
    var lendingLibraryCode = dcbItem.getLendingLibraryCode();
    if (StringUtils.isBlank(lendingLibraryCode)) {
      log.debug("tryFetchLocationIdByLendingLibraryCode:: " +
        "Lending library code is blank, shadow location fetch skipped");
      return Optional.empty();
    }

    return findLibraryLocationId(lendingLibraryCode)
      .flatMap(this::findLocationCodeByLibraryId);
  }

  private Optional<String> findLibraryLocationId(String lendingLibraryCode) {
    log.debug("findLibraryLocationId:: Fetching library by code: {}.", lendingLibraryCode);
    var libraryQuery = CqlQuery.exactMatch("code", lendingLibraryCode);
    var librariesResultByQuery = locationUnitClient.findLibrariesByQuery(libraryQuery, true, 1, 0);
    var librariesByCode = librariesResultByQuery.getResult();
    if (CollectionUtils.isEmpty(librariesByCode)) {
      log.debug("findLibraryLocationId:: No library found for lending library code: {}.", lendingLibraryCode);
      return Optional.empty();
    }

    var libraryByCode = librariesByCode.getFirst();
    return Optional.ofNullable(libraryByCode.getId());
  }

  private Optional<String> findLocationCodeByLibraryId(String libraryId) {
    log.debug("findLocationCodeByLibraryId:: Fetching location by lending library id: {}.", libraryId);
    var libraryQuery = CqlQuery.exactMatch("libraryId", libraryId);
    var locationsResultByQuery = locationsClient.findLocationByQuery(libraryQuery, true, 1, 0);
    var locationsByQuery = locationsResultByQuery.getResult();
    if (CollectionUtils.isEmpty(locationsByQuery)) {
      log.debug("findLocationCodeByLibraryId:: No location found for lending library id: {}.", libraryId);
      return Optional.empty();
    }

    return Optional.ofNullable(locationsByQuery.getFirst()).map(LocationDTO::getId);
  }

  private String getDefaultDcbLocationId() {
    var locationId = dcbEntityService.findOrCreateLocation().getId();
    log.debug("getDefaultDcbLocationId:: Falling back to default "
      + "location id: {}, code: {}, name: {}", locationId, CODE, NAME);
    return locationId;
  }
}
