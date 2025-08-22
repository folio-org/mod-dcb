package org.folio.dcb.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.dcb.client.feign.DcbHubLocationClient;
import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.client.feign.LocationUnitClient;
import static org.folio.dcb.client.feign.LocationUnitClient.LocationUnit;
import static org.folio.dcb.utils.DCBConstants.NAME;
import static org.folio.dcb.utils.DcbHubLocationsGroupingUtil.groupByAgency;

import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.config.DcbHubProperties;
import org.folio.dcb.domain.dto.LocationUnitsStatus;
import org.folio.dcb.domain.dto.LocationsStatus;
import org.folio.dcb.domain.dto.RefreshShadowLocationResponse;
import org.folio.dcb.domain.dto.RefreshShadowLocationResponseLocationUnits;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.exception.DcbHubLocationException;
import org.folio.dcb.exception.ServiceException;
import org.folio.dcb.integration.keycloak.DcbHubKCCredentialSecureStore;
import org.folio.dcb.integration.keycloak.DcbHubKCTokenService;
import org.folio.dcb.integration.keycloak.model.DcbHubKCCredentials;
import org.folio.dcb.model.AgencyKey;
import org.folio.dcb.model.DcbHubLocationResponse;
import org.folio.dcb.model.LocationAgenciesIds;
import org.folio.dcb.model.LocationCodeNamePair;
import org.folio.dcb.service.DcbHubLocationService;
import org.folio.spring.model.ResultList;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class DcbHubLocationServiceImpl implements DcbHubLocationService {

  private final DcbHubKCTokenService dcbHubKCTokenService;
  private final DcbHubKCCredentialSecureStore dcbHubKCCredentialSecureStore;
  private final DcbHubLocationClient dcbHubLocationClient;
  private final LocationsClient locationsClient;
  private final LocationUnitClient locationUnitClient;
  private final InventoryServicePointClient servicePointClient;
  private final DcbHubProperties dcbHubProperties;

  @Value("${application.dcb-hub.batch-size}")
  private Integer batchSize;

  @Override
  public RefreshShadowLocationResponse createShadowLocations() {
    try {
      log.debug("createShadowLocations:: creating shadow locations");

      if (!Boolean.TRUE.equals(dcbHubProperties.getFetchDcbLocationsEnabled())) {
        log.info("createShadowLocations:: DCB Hub locations fetching is disabled, skipping shadow location creation");
        throw new DcbHubLocationException("DCB Hub locations fetching is disabled, skipping shadow location creation", HttpStatus.BAD_REQUEST);
      }

      List<ServicePointRequest> servicePointList = servicePointClient.getServicePointByName(NAME).getResult();
      if (servicePointList.isEmpty()) {
        log.info(
          "createShadowLocations:: No service points found with name {}, So cannot create shadow locations without {} service point",
          NAME, NAME);
        throw new DcbHubLocationException(String.format("No service points found with name %s, So cannot create shadow locations without %s service point", NAME, NAME), HttpStatus.NOT_FOUND);
      }

      ServicePointRequest servicePointRequest = servicePointList.getFirst();
      log.info("createShadowLocations:: fetching all locations from DCB Hub");

      List<DcbHubLocationResponse.Location> locationList = fetchDcbHubAllLocations();
      if (CollectionUtils.isEmpty(locationList)) {
        log.info("createShadowLocations:: No locations found in DCB Hub, skipping shadow location creation");
        throw new DcbHubLocationException("No locations found in DCB Hub, skipping shadow location creation", HttpStatus.NOT_FOUND);
      }

      RefreshShadowLocationResponse refreshShadowLocationResponse = new RefreshShadowLocationResponse()
        .locationUnits(new RefreshShadowLocationResponseLocationUnits());
      createShadowLocations(servicePointRequest, locationList, refreshShadowLocationResponse);
      log.info("createShadowLocations:: shadow locations created");
      return refreshShadowLocationResponse;
    } catch (FeignException e) {
      log.error("createShadowLocations:: FeignException while fetching locations from DCB Hub", e);
      throw new DcbHubLocationException("FeignException while fetching locations from DCB Hub", HttpStatus.resolve(e.status()), e);
    } catch (ServiceException | DcbHubLocationException e) {
      throw e;
    } catch (Exception e) {
      log.error("createShadowLocations:: Exception while fetching locations from DCB Hub", e);
      throw new DcbHubLocationException("Exception while fetching locations from DCB Hub", HttpStatus.INTERNAL_SERVER_ERROR, e);
    }
  }

  private void createShadowLocations(ServicePointRequest servicePointRequest, List<DcbHubLocationResponse.Location> locations,
    RefreshShadowLocationResponse refreshShadowLocationResponse) {
    Map<AgencyKey, List<LocationCodeNamePair>> locationsGroupedByAgency =
      groupByAgency(locations);

    Map<AgencyKey, LocationAgenciesIds> agencyLocationUnitMapping =
      createLocationUnits(locationsGroupedByAgency.keySet(), refreshShadowLocationResponse);

    createShadowLocationEntries(locationsGroupedByAgency, agencyLocationUnitMapping, servicePointRequest, refreshShadowLocationResponse);
  }

  private Map<AgencyKey, LocationAgenciesIds> createLocationUnits(
    Set<AgencyKey> agencies, RefreshShadowLocationResponse refreshShadowLocationResponse) {

    Map<AgencyKey, LocationAgenciesIds> agencyLocationUnitMapping =
      new HashMap<>();

    agencies.forEach(agencyKey -> {
      log.debug("createLocationUnits:: Creating units for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());

      LocationUnit institution = createInstitution(agencyKey, refreshShadowLocationResponse);
      LocationUnit campus = createCampus(agencyKey, institution, refreshShadowLocationResponse);
      LocationUnit library = createLibrary(agencyKey, campus, refreshShadowLocationResponse);

      agencyLocationUnitMapping.put(agencyKey, new LocationAgenciesIds(
        institution != null ? institution.getId() : null,
        campus != null ? campus.getId() : null,
        library != null ? library.getId() : null)
      );
    });

    return agencyLocationUnitMapping;
  }

  private LocationUnit createInstitution(AgencyKey agencyKey, RefreshShadowLocationResponse refreshShadowLocationResponse) {
    try {
      ResultList<LocationUnit> locationUnitResultList =
        locationUnitClient.findInstitutionsByQuery(formatAgencyQuery(agencyKey.agencyName(), agencyKey.agencyCode()), true,10, 0);

      if (CollectionUtils.isNotEmpty(locationUnitResultList.getResult())) {
        log.info("createInstitution:: Institution already exists for agency: {} - {}",
          agencyKey.agencyCode(), agencyKey.agencyName());
        addInstitutionsResponse(refreshShadowLocationResponse, agencyKey, LocationUnitsStatus.StatusEnum.SKIPPED, null);
        return locationUnitResultList.getResult().getFirst();
      }

      LocationUnit institution = LocationUnit.builder()
        .id(UUID.randomUUID().toString())
        .name(agencyKey.agencyName())
        .code(agencyKey.agencyCode())
        .isShadow(true)
        .build();
      locationUnitClient.createInstitution(institution);
      addInstitutionsResponse(refreshShadowLocationResponse, agencyKey, LocationUnitsStatus.StatusEnum.SUCCESS, null);
      log.debug("createInstitution:: Created institution: {} - {}", institution.getCode(), institution.getName());
      return institution;
    } catch (Exception e) {
      log.error("createInstitution:: Error creating institution for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName(), e);
      addInstitutionsResponse(refreshShadowLocationResponse, agencyKey, LocationUnitsStatus.StatusEnum.ERROR, e.getMessage());
      return null;
    }
  }

  private LocationUnit createCampus(
    AgencyKey agencyKey, LocationUnit institution, RefreshShadowLocationResponse refreshShadowLocationResponse) {
    try {
      if (institution == null) {
        log.error("createCampus:: Institution is null for agency: {} - {}, cannot create campus",
          agencyKey.agencyCode(), agencyKey.agencyName());
        addCampusResponse(refreshShadowLocationResponse, agencyKey, LocationUnitsStatus.StatusEnum.SKIPPED,
          "Institution is null and it was not created, so cannot create campus");
        return null;
      }
      ResultList<LocationUnit> locationUnitResultList =
        locationUnitClient.findCampusesByQuery(
          formatAgencyQuery(agencyKey.agencyName(), agencyKey.agencyCode()), true, 10, 0);

      if (!locationUnitResultList.getResult().isEmpty()) {
        log.info("createCampus:: campus already exists for agency: {} - {}",
          agencyKey.agencyCode(), agencyKey.agencyName());
        addCampusResponse(refreshShadowLocationResponse, agencyKey, LocationUnitsStatus.StatusEnum.SKIPPED, null);
        return locationUnitResultList.getResult().getFirst();
      }

      LocationUnit campus = LocationUnit.builder()
        .institutionId(institution.getId())
        .id(UUID.randomUUID().toString())
        .name(agencyKey.agencyName())
        .code(agencyKey.agencyCode())
        .isShadow(true)
        .build();
      locationUnitClient.createCampus(campus);
      addCampusResponse(refreshShadowLocationResponse, agencyKey, LocationUnitsStatus.StatusEnum.SUCCESS, null);
      log.debug("createCampus:: Created campus: {} - {}", campus.getCode(), campus.getName());
      return campus;
    } catch (Exception e) {
      log.error("createCampus:: Error creating campus for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName(), e);
      addCampusResponse(refreshShadowLocationResponse, agencyKey, LocationUnitsStatus.StatusEnum.ERROR, e.getMessage());
      return null;
    }
  }

  private LocationUnit createLibrary(
    AgencyKey agencyKey, LocationUnit campus, RefreshShadowLocationResponse refreshShadowLocationResponse) {
    try {
      if (campus == null) {
        log.error("createLibrary:: Campus is null for agency: {} - {}, cannot create library",
          agencyKey.agencyCode(), agencyKey.agencyName());
        addLibraryResponse(refreshShadowLocationResponse, agencyKey, LocationUnitsStatus.StatusEnum.SKIPPED,
          "Campus is null and it was not created, so cannot create library");
        return null;
      }
      ResultList<LocationUnit> locationUnitResultList =
        locationUnitClient.findLibrariesByQuery(
          formatAgencyQuery(agencyKey.agencyName(), agencyKey.agencyCode()), true, 10, 0);

      if (!locationUnitResultList.getResult().isEmpty()) {
        log.info("createLibrary:: library already exists for agency: {} - {}",
          agencyKey.agencyCode(), agencyKey.agencyName());
        addLibraryResponse(refreshShadowLocationResponse, agencyKey, LocationUnitsStatus.StatusEnum.SKIPPED, null);
        return locationUnitResultList.getResult().getFirst();
      }

      LocationUnit library = LocationUnit.builder()
        .campusId(campus.getId())
        .id(UUID.randomUUID().toString())
        .name(agencyKey.agencyName())
        .code(agencyKey.agencyCode())
        .isShadow(true)
        .build();
      locationUnitClient.createLibrary(library);
      addLibraryResponse(refreshShadowLocationResponse, agencyKey, LocationUnitsStatus.StatusEnum.SUCCESS, null);
      log.debug("createLibrary:: Created library: {} - {}", library.getCode(), library.getName());
      return library;
    } catch (Exception e) {
      log.error("createLibrary:: Error creating library for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName(), e);
      addLibraryResponse(refreshShadowLocationResponse, agencyKey, LocationUnitsStatus.StatusEnum.ERROR, e.getMessage());
      return null;
    }
  }

  private @NotNull String formatAgencyQuery(String name, String code) {
    return String.format("(name==%s AND code==%s)", name, code);
  }

  private void createShadowLocationEntries(
    Map<AgencyKey, List<LocationCodeNamePair>> locationsGroupedByAgency,
    Map<AgencyKey, LocationAgenciesIds> agencyLocationUnitMapping,
    ServicePointRequest servicePointRequest, RefreshShadowLocationResponse refreshShadowLocationResponse) {

    locationsGroupedByAgency.forEach((agencyKey, locationCodeNamePairs) -> {
      log.debug("createShadowLocationEntries:: Processing agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());

      LocationAgenciesIds locationAgenciesIds = agencyLocationUnitMapping.get(agencyKey);
      locationCodeNamePairs.forEach(
        location -> createShadowLocation(location, locationAgenciesIds, servicePointRequest, refreshShadowLocationResponse));
    });
  }

  private void createShadowLocation(
    LocationCodeNamePair location,
    LocationAgenciesIds locationAgenciesIds, ServicePointRequest servicePointRequest,
    RefreshShadowLocationResponse refreshShadowLocationResponse) {
    try {
      if (locationAgenciesIds.institutionId() == null ||
          locationAgenciesIds.libraryId() == null ||
          locationAgenciesIds.campusId() == null) {
        log.error("createShadowLocation:: Location agencies IDs are incomplete or null for location: {} - {}, cannot create shadow location. locationAgenciesIds are: {}",
          location.code(), location.name(), locationAgenciesIds.toString());
        addLocationsResponse(refreshShadowLocationResponse, location, LocationsStatus.StatusEnum.SKIPPED,
          String.format("Location agencies IDs are incomplete or null, cannot create shadow location. locationAgenciesIds are: %s", locationAgenciesIds));
        return;
      }

      ResultList<LocationsClient.LocationDTO> locationDTOResultList =
        locationsClient.findLocationByQuery(
          formatAgencyQuery(location.name(), location.code()), true, 10, 0);
      if (!locationDTOResultList.getResult().isEmpty()) {
        log.info("createShadowLocation:: Location already exists: {} - {}, skipping...", location.code(), location.name());
        addLocationsResponse(refreshShadowLocationResponse, location, LocationsStatus.StatusEnum.SKIPPED, null);
        return;
      }

      log.debug("createShadowLocation:: Creating shadow location: {} - {}",
        location.code(), location.name());

      LocationsClient.LocationDTO shadowLocation = LocationsClient.LocationDTO.builder()
        .id(UUID.randomUUID().toString())
        .code(location.code())
        .name(location.name())
        .institutionId(locationAgenciesIds.institutionId())
        .campusId(locationAgenciesIds.campusId())
        .libraryId(locationAgenciesIds.libraryId())
        .primaryServicePoint(servicePointRequest.getId())
        .servicePointIds(List.of(servicePointRequest.getId()))
        .isShadow(true)
        .build();

      locationsClient.createLocation(shadowLocation);
      addLocationsResponse(refreshShadowLocationResponse, location, LocationsStatus.StatusEnum.SUCCESS, null);
      log.debug("createShadowLocation:: Created shadow location: {} - {}",
        shadowLocation.getCode(), shadowLocation.getName());
    } catch (Exception e) {
      log.error("createShadowLocation:: Unexpected error creating shadow location: {} - {}",
        location.code(), location.name(), e);
      addLocationsResponse(refreshShadowLocationResponse, location, LocationsStatus.StatusEnum.ERROR, e.getMessage());
    }
  }

  private List<DcbHubLocationResponse.Location> fetchDcbHubAllLocations() {
    log.info("fetchDcbHubAllLocations:: fetching all locations from DCB Hub");

    int pageNumber = 1;
    int pageSize = batchSize;
    DcbHubKCCredentials dcbHubKCCredentials = dcbHubKCCredentialSecureStore.getDcbHubKCCredentials();
    List<DcbHubLocationResponse.Location> allLocations = new ArrayList<>();

    while (true) {
      String bearerToken = dcbHubKCTokenService.getBearerAccessToken(dcbHubKCCredentials);
      DcbHubLocationResponse response = dcbHubLocationClient.getLocations(pageNumber, pageSize, bearerToken);

      if (CollectionUtils.isNotEmpty(response.getContent())) {
        allLocations.addAll(response.getContent());
      }
      // Stop if last page reached
      if (pageNumber >= response.getTotalPages()) {
        break;
      }

      pageNumber++;
    }
    log.debug("fetchDcbHubAllLocations:: successfully fetched {} locations from DCB Hub", allLocations.size());
    return allLocations;
  }

  private static void addInstitutionsResponse(RefreshShadowLocationResponse refreshShadowLocationResponse, AgencyKey agencyKey,
    LocationUnitsStatus.StatusEnum statusEnum, String cause) {
    Objects.requireNonNull(refreshShadowLocationResponse.getLocationUnits())
      .addInstitutionsItem(LocationUnitsStatus.builder()
        .code(agencyKey.agencyCode())
        .status(statusEnum)
        .cause(cause).build());
  }

  private static void addCampusResponse(RefreshShadowLocationResponse refreshShadowLocationResponse, AgencyKey agencyKey,
    LocationUnitsStatus.StatusEnum statusEnum, String cause) {
    Objects.requireNonNull(refreshShadowLocationResponse.getLocationUnits())
      .addCampusesItem(LocationUnitsStatus.builder()
        .code(agencyKey.agencyCode())
        .status(statusEnum)
        .cause(cause).build());
  }

  private static void addLibraryResponse(RefreshShadowLocationResponse refreshShadowLocationResponse, AgencyKey agencyKey,
    LocationUnitsStatus.StatusEnum statusEnum, String cause) {
    Objects.requireNonNull(refreshShadowLocationResponse.getLocationUnits())
      .addLibrariesItem(LocationUnitsStatus.builder()
        .code(agencyKey.agencyCode())
        .status(statusEnum)
        .cause(cause).build());
  }

  private static void addLocationsResponse(RefreshShadowLocationResponse refreshShadowLocationResponse,
    LocationCodeNamePair locationCodeNamePair, LocationsStatus.StatusEnum statusEnum, String cause) {
    refreshShadowLocationResponse.getLocations()
      .add(LocationsStatus.builder()
        .code(locationCodeNamePair.code())
        .status(statusEnum)
        .cause(cause).build());
  }
}
