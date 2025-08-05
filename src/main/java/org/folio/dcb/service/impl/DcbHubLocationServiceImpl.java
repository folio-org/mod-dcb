package org.folio.dcb.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.folio.dcb.client.feign.DcbHubLocationClient;
import org.folio.dcb.client.feign.LocationUnitClient;
import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.exception.ServiceException;
import org.folio.dcb.integration.keycloak.DcbHubKCCredentialSecureStore;
import org.folio.dcb.integration.keycloak.DcbHubKCTokenService;
import org.folio.dcb.integration.keycloak.model.DcbHubKCCredentials;
import org.folio.dcb.model.LocationResponse;
import org.folio.dcb.service.DcbHubLocationService;
import org.folio.dcb.utils.DcbHubLocationsGroupingUtil;
import org.folio.spring.model.ResultList;
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

  @Value("${application.dcb-hub.batch-size}")
  private Integer batchSize;

  @Override
  public void createShadowLocations(LocationsClient locationsClient, LocationUnitClient locationUnitClient,
    ServicePointRequest servicePointRequest) {
    try {
      log.info("fetchDcbHubAllLocations:: fetching all locations from DCB Hub");

      int pageNumber = 1;
      int pageSize = batchSize; // default page size
      DcbHubKCCredentials dcbHubKCCredentials = dcbHubKCCredentialSecureStore.getDcbHubKCCredentials();

      log.info("fetchDcbHubAllLocations:: fetching all locations from DCB Hub");
      while (true) {
        String bearerToken = dcbHubKCTokenService.getBearerAccessToken(dcbHubKCCredentials);
        LocationResponse response = dcbHubLocationClient.getLocations(pageNumber, pageSize, bearerToken);

        if (Objects.nonNull(response.getContent()) && !response.getContent().isEmpty()) {
          createShadowLocations(locationsClient, locationUnitClient, servicePointRequest, response.getContent());
        }
        // Stop if last page reached
        if (pageNumber >= response.getTotalPages()) {
          break;
        }

        pageNumber++;
      }
    } catch (FeignException e) {
      log.error("fetchDcbHubAllLocations:: FeignException while fetching locations from DCB Hub", e);
      throw new ServiceException("FeignException while fetching locations from DCB Hub", e);
    } catch (Exception e) {
      log.error("fetchDcbHubAllLocations:: Exception while fetching locations from DCB Hub", e);
      throw new ServiceException("Exception while fetching locations from DCB Hub", e);
    }
  }

  private void createShadowLocations(LocationsClient locationsClient, LocationUnitClient locationUnitClient,
    ServicePointRequest servicePointRequest, List<LocationResponse.Location> locations) {

    if (locations == null || locations.isEmpty()) {
      log.warn("createShadowLocations:: No locations found in DCB Hub");
      return;
    }

    Map<DcbHubLocationsGroupingUtil.AgencyKey, List<DcbHubLocationsGroupingUtil.LocationCodeNamePair>> locationsGroupedByAgency =
      DcbHubLocationsGroupingUtil.groupByAgency(locations);

    Map<DcbHubLocationsGroupingUtil.AgencyKey, DcbHubLocationsGroupingUtil.LocationAgenciesIds> agencyLocationUnitMapping =
      createLocationUnits(locationUnitClient, locationsGroupedByAgency.keySet());

    createShadowLocationEntries(locationsClient, locationsGroupedByAgency, agencyLocationUnitMapping, servicePointRequest);
  }

  private Map<DcbHubLocationsGroupingUtil.AgencyKey, DcbHubLocationsGroupingUtil.LocationAgenciesIds> createLocationUnits(
    LocationUnitClient locationUnitClient,
    Set<DcbHubLocationsGroupingUtil.AgencyKey> agencies) {

    Map<DcbHubLocationsGroupingUtil.AgencyKey, DcbHubLocationsGroupingUtil.LocationAgenciesIds> agencyLocationUnitMapping =
      new HashMap<>();

    agencies.forEach(agencyKey -> {
      log.debug("createLocationUnits:: Creating units for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());

      LocationUnitClient.LocationUnit institution = createInstitution(locationUnitClient, agencyKey);
      LocationUnitClient.LocationUnit campus = createCampus(locationUnitClient, agencyKey, institution.getId());
      LocationUnitClient.LocationUnit library = createLibrary(locationUnitClient, agencyKey, campus.getId());

      agencyLocationUnitMapping.put(agencyKey, new DcbHubLocationsGroupingUtil.LocationAgenciesIds(
        institution.getId(), campus.getId(), library.getId()));
    });

    return agencyLocationUnitMapping;
  }

  private LocationUnitClient.LocationUnit createInstitution(
    LocationUnitClient locationUnitClient,
    DcbHubLocationsGroupingUtil.AgencyKey agencyKey) {

    ResultList<LocationUnitClient.LocationUnit> locationUnitResultList =
      locationUnitClient.queryInstitutionByNameAndCode(agencyKey.agencyName(), agencyKey.agencyCode());

    if (!locationUnitResultList.getResult().isEmpty()) {
      log.info("createInstitution:: Institution already exists for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());
      return locationUnitResultList.getResult().getFirst();
    }

    LocationUnitClient.LocationUnit institution = LocationUnitClient.LocationUnit.builder()
      .id(UUID.randomUUID().toString())
      .name(agencyKey.agencyName())
      .code(agencyKey.agencyCode())
      .build();
    locationUnitClient.createInstitution(institution);
    log.debug("createInstitution:: Created institution: {} - {}", institution.getCode(), institution.getName());
    return institution;
  }

  private LocationUnitClient.LocationUnit createCampus(
    LocationUnitClient locationUnitClient,
    DcbHubLocationsGroupingUtil.AgencyKey agencyKey,
    String institutionId) {

    ResultList<LocationUnitClient.LocationUnit> locationUnitResultList =
      locationUnitClient.queryCampusByNameAndCode(agencyKey.agencyName(), agencyKey.agencyCode());

    if (!locationUnitResultList.getResult().isEmpty()) {
      log.info("createCampus:: campus already exists for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());
      return locationUnitResultList.getResult().getFirst();
    }

    LocationUnitClient.LocationUnit campus = LocationUnitClient.LocationUnit.builder()
      .institutionId(institutionId)
      .id(UUID.randomUUID().toString())
      .name(agencyKey.agencyName())
      .code(agencyKey.agencyCode())
      .build();
    locationUnitClient.createCampus(campus);
    log.debug("createCampus:: Created campus: {} - {}", campus.getCode(), campus.getName());
    return campus;
  }

  private LocationUnitClient.LocationUnit createLibrary(
    LocationUnitClient locationUnitClient,
    DcbHubLocationsGroupingUtil.AgencyKey agencyKey,
    String campusId) {

    ResultList<LocationUnitClient.LocationUnit> locationUnitResultList =
      locationUnitClient.queryLibraryByNameAndCode(agencyKey.agencyName(), agencyKey.agencyCode());

    if (!locationUnitResultList.getResult().isEmpty()) {
      log.debug("createLibrary:: library already exists for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());
      return locationUnitResultList.getResult().getFirst();
    }

    LocationUnitClient.LocationUnit library = LocationUnitClient.LocationUnit.builder()
      .campusId(campusId)
      .id(UUID.randomUUID().toString())
      .name(agencyKey.agencyName())
      .code(agencyKey.agencyCode())
      .build();
    locationUnitClient.createLibrary(library);
    log.debug("createLibrary:: Created library: {} - {}", library.getCode(), library.getName());
    return library;
  }

  private void createShadowLocationEntries(
    LocationsClient locationsClient,
    Map<DcbHubLocationsGroupingUtil.AgencyKey, List<DcbHubLocationsGroupingUtil.LocationCodeNamePair>> locationsGroupedByAgency,
    Map<DcbHubLocationsGroupingUtil.AgencyKey, DcbHubLocationsGroupingUtil.LocationAgenciesIds> agencyLocationUnitMapping,
    ServicePointRequest servicePointRequest) {

    locationsGroupedByAgency.forEach((agencyKey, locationCodeNamePairs) -> {
      log.debug("createShadowLocationEntries:: Processing agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());

      DcbHubLocationsGroupingUtil.LocationAgenciesIds locationAgenciesIds = agencyLocationUnitMapping.get(agencyKey);
      locationCodeNamePairs.forEach(location -> createShadowLocation(
        locationsClient, location, locationAgenciesIds, servicePointRequest));
    });
  }

  private void createShadowLocation(
    LocationsClient locationsClient,
    DcbHubLocationsGroupingUtil.LocationCodeNamePair location,
    DcbHubLocationsGroupingUtil.LocationAgenciesIds locationAgenciesIds, ServicePointRequest servicePointRequest) {

    ResultList<LocationsClient.LocationDTO> locationDTOResultList =
      locationsClient.queryLocationsByNameAndCode(location.name(), location.code());
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
}
