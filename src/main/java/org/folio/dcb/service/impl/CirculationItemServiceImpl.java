package org.folio.dcb.service.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.dcb.domain.dto.ItemStatus.NameEnum.IN_TRANSIT;
import static org.folio.dcb.utils.DcbConstants.CODE;
import static org.folio.dcb.utils.DcbConstants.MATERIAL_TYPE_NAME_BOOK;
import static org.folio.dcb.utils.DcbConstants.NAME;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.dcb.config.DcbFeatureProperties;
import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.ItemStatus;
import org.folio.dcb.integration.circitem.CirculationItemClient;
import org.folio.dcb.integration.invstorage.LocationUnitClient;
import org.folio.dcb.integration.invstorage.LocationsClient;
import org.folio.dcb.integration.invstorage.model.Location;
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
  private final DcbFeatureProperties dcbFeatureProperties;
  private final DcbEntityServiceFacade dcbEntityService;

  @Override
  public CirculationItem checkIfItemExistsAndCreate(DcbItem dcbItem, String pickupServicePointId) {
    return checkIfItemExistsAndCreate(dcbItem, pickupServicePointId, false);
  }

  @Override
  public CirculationItem checkIfItemExistsAndCreate(DcbItem dcbItem, String pickupServicePointId,
    boolean useRealItemId) {

    log.debug("checkIfItemExistsAndCreate:: generating a circulation item if it does not exist.");
    var circulationItem = fetchCirculationItemByBarcode(dcbItem.getBarcode());
    if (circulationItem == null) {
      log.warn("checkIfItemExistsAndCreate:: Circulation item not found. Creating it.");
      var effectiveLocationId = fetchShadowLocationForItem(dcbItem);
      return createCirculationItem(dcbItem, pickupServicePointId, effectiveLocationId, useRealItemId);
    }

    return updateItemLocationIfChanged(circulationItem, dcbItem);
  }

  private CirculationItem updateItemLocationIfChanged(CirculationItem item, DcbItem dcbItem) {
    if (!dcbFeatureProperties.isFlexibleCirculationRulesEnabled()) {
      log.debug("updateItemLocationIfChanged:: Shadow location lookup disabled, skipping update");
      return item;
    }
    var newEffectiveLocationId = resolveEffectiveLocationId(dcbItem);
    if (Objects.equals(newEffectiveLocationId, item.getEffectiveLocationId())) {
      log.debug("updateItemLocationIfChanged:: Effective location unchanged for item {}.", item.getId());
      return item;
    }
    log.info("updateItemLocationIfChanged:: Effective location changed for item {}. "
      + "Updating from {} to {}.", item.getId(), item.getEffectiveLocationId(), newEffectiveLocationId);
    item.setEffectiveLocationId(newEffectiveLocationId);
    return circulationItemClient.updateCirculationItem(item.getId(), item);
  }

  private String fetchShadowLocationForItem(DcbItem dcbItem) {
    if (dcbFeatureProperties.isFlexibleCirculationRulesEnabled()) {
      return resolveEffectiveLocationId(dcbItem);
    }

    log.debug("fetchShadowLocationForItem:: Shadow location lookup is disabled");
    return getDefaultDcbLocationId();
  }

  private String resolveEffectiveLocationId(DcbItem dcbItem) {
    return tryFetchLocationIdByLocationCode(dcbItem)
      .or(() -> tryFetchLocationIdByLendingLibraryCode(dcbItem))
      .orElseGet(this::getDefaultDcbLocationId);
  }

  private CirculationItem fetchCirculationItemByBarcode(String barcode) {
    var query = CqlQuery.exactMatch("barcode", barcode).getQuery();
    return circulationItemClient.fetchItemByCqlQuery(query)
      .getItems()
      .stream()
      .findFirst()
      .orElse(null);
  }

  public CirculationItem fetchItemById(String itemId) {
    log.info("fetchItemById:: fetching item details for id {} ", itemId);
    return circulationItemClient.retrieveCirculationItemById(itemId);
  }

  private CirculationItem createCirculationItem(DcbItem item, String pickupServicePointId,
    String effectiveLocationId, boolean useRealItemId) {

    var materialType = isBlank(item.getMaterialType()) ? MATERIAL_TYPE_NAME_BOOK : item.getMaterialType();
    var materialTypeId = itemService.fetchItemMaterialTypeIdByMaterialTypeName(materialType);
    var dcbHolding = dcbEntityService.findOrCreateHolding();
    var itemId = useRealItemId ? item.getId() : UUID.randomUUID().toString();
    var loanType = dcbEntityService.findOrCreateLoanType();
    var circulationItem = new CirculationItem()
      .id(itemId)
      .barcode(item.getBarcode())
      .status(new ItemStatus().name(IN_TRANSIT))
      .holdingsRecordId(dcbHolding.getId())
      .instanceTitle(item.getTitle())
      .materialTypeId(materialTypeId)
      .permanentLoanTypeId(loanType.getId())
      .pickupLocation(pickupServicePointId)
      .lendingLibraryCode(item.getLendingLibraryCode())
      .effectiveLocationId(effectiveLocationId);

    return circulationItemClient.createCirculationItem(itemId, circulationItem);
  }

  private Optional<String> tryFetchLocationIdByLocationCode(DcbItem dcbItem) {
    var locationCode = dcbItem.getLocationCode();
    if (isBlank(locationCode)) {
      log.debug("tryFetchLocationIdByLocationCode:: Location code is blank, shadow location fetch skipped.");
      return Optional.empty();
    }

    log.debug("tryFetchLocationIdByLocationCode:: Fetching shadow location id by location code: {}", locationCode);
    var query = CqlQuery.exactMatchByCode(locationCode).getQuery();
    var locationDtoResult = locationsClient.findLocationByQuery(query, true, 1, 0);
    if (locationDtoResult.getResult().isEmpty()) {
      log.debug("tryFetchLocationIdByLocationCode:: No shadow location found for code: {}.", locationCode);
      return Optional.empty();
    }

    var locationDto = locationDtoResult.getResult().getFirst();
    log.debug("tryFetchLocationIdByLocationCode:: "
        + "Shadow location lookup is enabled. Found location for code: {} with id: {}",
      locationCode, locationDto.getId());
    return Optional.ofNullable(locationDto.getId());
  }

  private Optional<String> tryFetchLocationIdByLendingLibraryCode(DcbItem dcbItem) {
    var lendingLibraryCode = dcbItem.getLendingLibraryCode();
    if (isBlank(lendingLibraryCode)) {
      log.debug("tryFetchLocationIdByLendingLibraryCode:: "
        + "Lending library code is blank, shadow location fetch skipped");
      return Optional.empty();
    }

    return findLibraryLocationId(lendingLibraryCode)
      .flatMap(this::findLocationCodeByLibraryId);
  }

  private Optional<String> findLibraryLocationId(String lendingLibraryCode) {
    log.debug("findLibraryLocationId:: Fetching library by code: {}.", lendingLibraryCode);
    var libraryQuery = CqlQuery.exactMatchByCode(lendingLibraryCode).getQuery();
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
    var libraryQuery = CqlQuery.exactMatch("libraryId", libraryId).getQuery();
    var locationsResultByQuery = locationsClient.findLocationByQuery(libraryQuery, true, 1, 0);
    var locationsByQuery = locationsResultByQuery.getResult();
    if (CollectionUtils.isEmpty(locationsByQuery)) {
      log.debug("findLocationCodeByLibraryId:: No location found for lending library id: {}.", libraryId);
      return Optional.empty();
    }

    return Optional.ofNullable(locationsByQuery.getFirst()).map(Location::getId);
  }

  private String getDefaultDcbLocationId() {
    var locationId = dcbEntityService.findOrCreateLocation().getId();
    log.debug("getDefaultDcbLocationId:: Falling back to default "
      + "location id: {}, code: {}, name: {}", locationId, CODE, NAME);
    return locationId;
  }
}
