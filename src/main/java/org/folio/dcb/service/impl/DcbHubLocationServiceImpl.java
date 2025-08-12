package org.folio.dcb.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.dcb.client.feign.DcbHubLocationClient;
import org.folio.dcb.client.feign.LocationUnitClient;
import static org.folio.dcb.client.feign.LocationUnitClient.LocationUnit;
import static org.folio.dcb.utils.DcbHubLocationsGroupingUtil.groupByAgency;

import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.domain.dto.ServicePointRequest;
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

  @Value("${application.dcb-hub.batch-size}")
  private Integer batchSize;

  @Override
  public void createShadowLocations(ServicePointRequest servicePointRequest) {
    try {
      log.info("createShadowLocations:: fetching all locations from DCB Hub");
      List<DcbHubLocationResponse.Location> locationList = fetchDcbHubAllLocations();
      if (CollectionUtils.isEmpty(locationList)) {
        log.info("createShadowLocations:: No locations found in DCB Hub, skipping shadow location creation");
        return;
      }
      createShadowLocations(locationsClient, locationUnitClient, servicePointRequest, locationList);
    } catch (FeignException e) {
      log.error("createShadowLocations:: FeignException while fetching locations from DCB Hub", e);
      throw new ServiceException("FeignException while fetching locations from DCB Hub", e);
    } catch (Exception e) {
      log.error("createShadowLocations:: Exception while fetching locations from DCB Hub", e);
      throw new ServiceException("Exception while fetching locations from DCB Hub", e);
    }
  }

  private void createShadowLocations(LocationsClient locationsClient, LocationUnitClient locationUnitClient,
    ServicePointRequest servicePointRequest, List<DcbHubLocationResponse.Location> locations) {
    Map<AgencyKey, List<LocationCodeNamePair>> locationsGroupedByAgency =
      groupByAgency(locations);

    Map<AgencyKey, LocationAgenciesIds> agencyLocationUnitMapping =
      createLocationUnits(locationUnitClient, locationsGroupedByAgency.keySet());

    createShadowLocationEntries(locationsClient, locationsGroupedByAgency, agencyLocationUnitMapping, servicePointRequest);
  }

  private Map<AgencyKey, LocationAgenciesIds> createLocationUnits(
    LocationUnitClient locationUnitClient,
    Set<AgencyKey> agencies) {

    Map<AgencyKey, LocationAgenciesIds> agencyLocationUnitMapping =
      new HashMap<>();

    agencies.forEach(agencyKey -> {
      log.debug("createLocationUnits:: Creating units for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());

      LocationUnit institution = createInstitution(locationUnitClient, agencyKey);
      LocationUnit campus = createCampus(locationUnitClient, agencyKey, institution.getId());
      LocationUnit library = createLibrary(locationUnitClient, agencyKey, campus.getId());

      agencyLocationUnitMapping.put(agencyKey, new LocationAgenciesIds(
        institution.getId(), campus.getId(), library.getId()));
    });

    return agencyLocationUnitMapping;
  }

  private LocationUnit createInstitution(
    LocationUnitClient locationUnitClient,
    AgencyKey agencyKey) {

    ResultList<LocationUnit> locationUnitResultList =
      locationUnitClient.findInstitutionsByQuery(formatAgencyQuery(agencyKey.agencyName(), agencyKey.agencyCode()), 10, 0);

    if (CollectionUtils.isNotEmpty(locationUnitResultList.getResult())) {
      log.info("createInstitution:: Institution already exists for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());
      return locationUnitResultList.getResult().getFirst();
    }

    LocationUnit institution = LocationUnit.builder()
      .id(UUID.randomUUID().toString())
      .name(agencyKey.agencyName())
      .code(agencyKey.agencyCode())
      .isShadow(true)
      .build();
    locationUnitClient.createInstitution(institution);
    log.debug("createInstitution:: Created institution: {} - {}", institution.getCode(), institution.getName());
    return institution;
  }

  private LocationUnit createCampus(
    LocationUnitClient locationUnitClient,
    AgencyKey agencyKey,
    String institutionId) {

    ResultList<LocationUnit> locationUnitResultList =
      locationUnitClient.findCampusesByQuery(
        formatAgencyQuery(agencyKey.agencyName(), agencyKey.agencyCode()), 10, 0);

    if (!locationUnitResultList.getResult().isEmpty()) {
      log.info("createCampus:: campus already exists for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());
      return locationUnitResultList.getResult().getFirst();
    }

    LocationUnit campus = LocationUnit.builder()
      .institutionId(institutionId)
      .id(UUID.randomUUID().toString())
      .name(agencyKey.agencyName())
      .code(agencyKey.agencyCode())
      .isShadow(true)
      .build();
    locationUnitClient.createCampus(campus);
    log.debug("createCampus:: Created campus: {} - {}", campus.getCode(), campus.getName());
    return campus;
  }

  private LocationUnit createLibrary(
    LocationUnitClient locationUnitClient,
    AgencyKey agencyKey,
    String campusId) {

    ResultList<LocationUnit> locationUnitResultList =
      locationUnitClient.findLibrariesByQuery(
        formatAgencyQuery(agencyKey.agencyName(), agencyKey.agencyCode()), 10, 0);

    if (!locationUnitResultList.getResult().isEmpty()) {
      log.info("createLibrary:: library already exists for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());
      return locationUnitResultList.getResult().getFirst();
    }

    LocationUnit library = LocationUnit.builder()
      .campusId(campusId)
      .id(UUID.randomUUID().toString())
      .name(agencyKey.agencyName())
      .code(agencyKey.agencyCode())
      .isShadow(true)
      .build();
    locationUnitClient.createLibrary(library);
    log.debug("createLibrary:: Created library: {} - {}", library.getCode(), library.getName());
    return library;
  }

  private @NotNull String formatAgencyQuery(String name, String code) {
    return String.format("(name==%s AND code==%s)", name, code);
  }

  private void createShadowLocationEntries(
    LocationsClient locationsClient,
    Map<AgencyKey, List<LocationCodeNamePair>> locationsGroupedByAgency,
    Map<AgencyKey, LocationAgenciesIds> agencyLocationUnitMapping,
    ServicePointRequest servicePointRequest) {

    locationsGroupedByAgency.forEach((agencyKey, locationCodeNamePairs) -> {
      log.debug("createShadowLocationEntries:: Processing agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());

      LocationAgenciesIds locationAgenciesIds = agencyLocationUnitMapping.get(agencyKey);
      locationCodeNamePairs.forEach(location -> createShadowLocation(
        locationsClient, location, locationAgenciesIds, servicePointRequest));
    });
  }

  private void createShadowLocation(
    LocationsClient locationsClient,
    LocationCodeNamePair location,
    LocationAgenciesIds locationAgenciesIds, ServicePointRequest servicePointRequest) {

    ResultList<LocationsClient.LocationDTO> locationDTOResultList =
      locationsClient.findLocationByQuery(
        formatAgencyQuery(location.name(), location.code()), true, 10, 0);
    if (!locationDTOResultList.getResult().isEmpty()) {
      log.info("createShadowLocation:: Location already exists: {} - {}, skipping...", location.code(), location.name());
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
    log.debug("createShadowLocation:: Created shadow location: {} - {}",
      shadowLocation.getCode(), shadowLocation.getName());
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
}
