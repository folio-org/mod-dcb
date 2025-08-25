package org.folio.dcb.service.impl;

import static org.folio.dcb.client.feign.LocationUnitClient.LocationUnit;
import static org.folio.dcb.utils.DCBConstants.NAME;
import static org.folio.dcb.utils.DcbHubLocationsGroupingUtil.groupByAgency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.dcb.client.feign.DcbHubLocationClient;
import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.client.feign.LocationUnitClient;
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

  public RefreshShadowLocationResponse createShadowLocations() {
    try {
      log.debug("createShadowLocations:: creating shadow locations");

      if (!Boolean.TRUE.equals(dcbHubProperties.getFetchDcbLocationsEnabled())) {
        log.info("createShadowLocations:: DCB Hub locations fetching is disabled, skipping shadow location creation");
        throw new DcbHubLocationException("DCB Hub locations fetching is disabled", HttpStatus.BAD_REQUEST);
      }

      List<ServicePointRequest> servicePointList = servicePointClient.getServicePointByName(NAME).getResult();
      if (servicePointList.isEmpty()) {
        log.info(
          "createShadowLocations:: No service points found with name {}, So cannot create shadow locations without {} service point",
          NAME, NAME);
        throw new DcbHubLocationException("DCB Service point is not found", HttpStatus.NOT_FOUND);
      }

      ServicePointRequest servicePointRequest = servicePointList.getFirst();
      log.info("createShadowLocations:: fetching all locations from DCB Hub");

      List<DcbHubLocationResponse.Location> locationList = fetchDcbHubAllLocations();
      if (CollectionUtils.isEmpty(locationList)) {
        log.warn("createShadowLocations:: No locations found in DCB Hub, skipping shadow location creation");
        return new RefreshShadowLocationResponse();
      }

      RefreshShadowLocationResponse response = createShadowLocations(servicePointRequest, locationList);
      log.info("createShadowLocations:: shadow locations created");
      return response;
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

  private RefreshShadowLocationResponse createShadowLocations(
    ServicePointRequest servicePointRequest,
    List<DcbHubLocationResponse.Location> locations) {

    Map<AgencyKey, List<LocationCodeNamePair>> locationsGroupedByAgency = groupByAgency(locations);

    LocationUnitsResult locationUnitsResult = createLocationUnits(locationsGroupedByAgency.keySet());
    List<LocationsStatus> locationStatuses = createShadowLocationEntries(
      locationsGroupedByAgency,
      locationUnitsResult.agencyLocationUnitMapping,
      servicePointRequest);

    return new RefreshShadowLocationResponse()
      .locationUnits(locationUnitsResult.locationUnits)
      .locations(locationStatuses);
  }

  private LocationUnitsResult createLocationUnits(Set<AgencyKey> agencies) {
    RefreshShadowLocationResponseLocationUnits locationUnits = new RefreshShadowLocationResponseLocationUnits();
    Map<AgencyKey, LocationAgenciesIds> agencyLocationUnitMapping = new HashMap<>();

    agencies.forEach(agencyKey -> {
      log.debug("createLocationUnits:: Creating units for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());

      InstitutionResult institutionResult = createInstitution(agencyKey);
      locationUnits.addInstitutionsItem(institutionResult.locationUnitsStatus);

      CampusResult campusResult = createCampus(agencyKey, institutionResult.institution);
      locationUnits.addCampusesItem(campusResult.locationUnitsStatus);

      LibraryResult libraryResult = createLibrary(agencyKey, campusResult.campus);
      locationUnits.addLibrariesItem(libraryResult.locationUnitsStatus);

      agencyLocationUnitMapping.put(agencyKey, new LocationAgenciesIds(
        institutionResult.institution != null ? institutionResult.institution.getId() : null,
        campusResult.campus != null ? campusResult.campus.getId() : null,
        libraryResult.library != null ? libraryResult.library.getId() : null)
      );
    });

    return new LocationUnitsResult(locationUnits, agencyLocationUnitMapping);
  }

  private record LocationUnitsResult(
    RefreshShadowLocationResponseLocationUnits locationUnits,
    Map<AgencyKey, LocationAgenciesIds> agencyLocationUnitMapping) {
  }

  private InstitutionResult createInstitution(AgencyKey agencyKey) {
    try {
      ResultList<LocationUnit> locationUnitResultList =
        locationUnitClient.findInstitutionsByQuery(formatAgencyQuery(agencyKey.agencyName(), agencyKey.agencyCode()), true, 10, 0);

      if (CollectionUtils.isNotEmpty(locationUnitResultList.getResult())) {
        log.info("createInstitution:: Institution already exists for agency: {} - {}",
          agencyKey.agencyCode(), agencyKey.agencyName());

        return new InstitutionResult(
          locationUnitResultList.getResult().getFirst(),
          LocationUnitsStatus.builder()
            .code(agencyKey.agencyCode())
            .status(LocationUnitsStatus.StatusEnum.SKIPPED)
            .build());
      }

      LocationUnit institution = LocationUnit.builder()
        .id(UUID.randomUUID().toString())
        .name(agencyKey.agencyName())
        .code(agencyKey.agencyCode())
        .isShadow(true)
        .build();
      locationUnitClient.createInstitution(institution);

      log.debug("createInstitution:: Created institution: {} - {}", institution.getCode(), institution.getName());

      return new InstitutionResult(
        institution,
        LocationUnitsStatus.builder()
          .code(agencyKey.agencyCode())
          .status(LocationUnitsStatus.StatusEnum.SUCCESS)
          .build());
    } catch (Exception e) {
      log.error("createInstitution:: Error creating institution for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName(), e);

      return new InstitutionResult(
        null,
        LocationUnitsStatus.builder()
          .code(agencyKey.agencyCode())
          .status(LocationUnitsStatus.StatusEnum.ERROR)
          .cause(e.getMessage())
          .build());
    }
  }

  private record InstitutionResult(LocationUnit institution, LocationUnitsStatus locationUnitsStatus) {}

  private CampusResult createCampus(AgencyKey agencyKey, LocationUnit institution) {
    try {
      if (institution == null) {
        log.error("createCampus:: Institution is null for agency: {} - {}, cannot create campus",
          agencyKey.agencyCode(), agencyKey.agencyName());

        return new CampusResult(
          null,
          LocationUnitsStatus.builder()
            .code(agencyKey.agencyCode())
            .status(LocationUnitsStatus.StatusEnum.SKIPPED)
            .cause("Institution is null and it was not created, so cannot create campus")
            .build());
      }

      ResultList<LocationUnit> locationUnitResultList =
        locationUnitClient.findCampusesByQuery(
          formatAgencyQuery(agencyKey.agencyName(), agencyKey.agencyCode()), true, 10, 0);

      if (!locationUnitResultList.getResult().isEmpty()) {
        log.info("createCampus:: campus already exists for agency: {} - {}",
          agencyKey.agencyCode(), agencyKey.agencyName());

        return new CampusResult(
          locationUnitResultList.getResult().getFirst(),
          LocationUnitsStatus.builder()
            .code(agencyKey.agencyCode())
            .status(LocationUnitsStatus.StatusEnum.SKIPPED)
            .build());
      }

      LocationUnit campus = LocationUnit.builder()
        .institutionId(institution.getId())
        .id(UUID.randomUUID().toString())
        .name(agencyKey.agencyName())
        .code(agencyKey.agencyCode())
        .isShadow(true)
        .build();
      locationUnitClient.createCampus(campus);

      log.debug("createCampus:: Created campus: {} - {}", campus.getCode(), campus.getName());

      return new CampusResult(
        campus,
        LocationUnitsStatus.builder()
          .code(agencyKey.agencyCode())
          .status(LocationUnitsStatus.StatusEnum.SUCCESS)
          .build());
    } catch (Exception e) {
      log.error("createCampus:: Error creating campus for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName(), e);

      return new CampusResult(
        null,
        LocationUnitsStatus.builder()
          .code(agencyKey.agencyCode())
          .status(LocationUnitsStatus.StatusEnum.ERROR)
          .cause(e.getMessage())
          .build());
    }
  }

  private record CampusResult(LocationUnit campus, LocationUnitsStatus locationUnitsStatus) {}

  private LibraryResult createLibrary(AgencyKey agencyKey, LocationUnit campus) {
    try {
      if (campus == null) {
        log.error("createLibrary:: Campus is null for agency: {} - {}, cannot create library",
          agencyKey.agencyCode(), agencyKey.agencyName());

        return new LibraryResult(
          null,
          LocationUnitsStatus.builder()
            .code(agencyKey.agencyCode())
            .status(LocationUnitsStatus.StatusEnum.SKIPPED)
            .cause("Campus is null and it was not created, so cannot create library")
            .build());
      }

      ResultList<LocationUnit> locationUnitResultList =
        locationUnitClient.findLibrariesByQuery(
          formatAgencyQuery(agencyKey.agencyName(), agencyKey.agencyCode()), true, 10, 0);

      if (!locationUnitResultList.getResult().isEmpty()) {
        log.info("createLibrary:: library already exists for agency: {} - {}",
          agencyKey.agencyCode(), agencyKey.agencyName());

        return new LibraryResult(
          locationUnitResultList.getResult().getFirst(),
          LocationUnitsStatus.builder()
            .code(agencyKey.agencyCode())
            .status(LocationUnitsStatus.StatusEnum.SKIPPED)
            .build());
      }

      LocationUnit library = LocationUnit.builder()
        .campusId(campus.getId())
        .id(UUID.randomUUID().toString())
        .name(agencyKey.agencyName())
        .code(agencyKey.agencyCode())
        .isShadow(true)
        .build();
      locationUnitClient.createLibrary(library);

      log.debug("createLibrary:: Created library: {} - {}", library.getCode(), library.getName());

      return new LibraryResult(
        library,
        LocationUnitsStatus.builder()
          .code(agencyKey.agencyCode())
          .status(LocationUnitsStatus.StatusEnum.SUCCESS)
          .build());
    } catch (Exception e) {
      log.error("createLibrary:: Error creating library for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName(), e);

      return new LibraryResult(
        null,
        LocationUnitsStatus.builder()
          .code(agencyKey.agencyCode())
          .status(LocationUnitsStatus.StatusEnum.ERROR)
          .cause(e.getMessage())
          .build());
    }
  }

  private record LibraryResult(LocationUnit library, LocationUnitsStatus locationUnitsStatus) {}

  private List<LocationsStatus> createShadowLocationEntries(
    Map<AgencyKey, List<LocationCodeNamePair>> locationsGroupedByAgency,
    Map<AgencyKey, LocationAgenciesIds> agencyLocationUnitMapping,
    ServicePointRequest servicePointRequest) {

    List<LocationsStatus> locationStatuses = new ArrayList<>();

    locationsGroupedByAgency.forEach((agencyKey, locationCodeNamePairs) -> {
      log.debug("createShadowLocationEntries:: Processing agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());

      LocationAgenciesIds locationAgenciesIds = agencyLocationUnitMapping.get(agencyKey);
      locationCodeNamePairs.forEach(location -> {
        LocationsStatus status = createShadowLocation(location, locationAgenciesIds, servicePointRequest);
        locationStatuses.add(status);
      });
    });

    return locationStatuses;
  }

  private LocationsStatus createShadowLocation(
    LocationCodeNamePair location,
    LocationAgenciesIds locationAgenciesIds, ServicePointRequest servicePointRequest) {
    try {
      if (locationAgenciesIds.institutionId() == null ||
          locationAgenciesIds.libraryId() == null ||
          locationAgenciesIds.campusId() == null) {
        log.error("createShadowLocation:: Location agencies IDs are incomplete or null for location: {} - {}, cannot create shadow location. locationAgenciesIds are: {}",
          location.code(), location.name(), locationAgenciesIds.toString());
        return LocationsStatus.builder()
          .code(location.code())
          .status(LocationsStatus.StatusEnum.SKIPPED)
          .cause(String.format(
            "Location agencies IDs are incomplete or null, cannot create shadow location. locationAgenciesIds are: %s",
            locationAgenciesIds))
          .build();
      }

      ResultList<LocationsClient.LocationDTO> locationDTOResultList =
        locationsClient.findLocationByQuery(
          formatAgencyQuery(location.name(), location.code()), true, 10, 0);
      if (!locationDTOResultList.getResult().isEmpty()) {
        log.info("createShadowLocation:: Location already exists: {} - {}, skipping...", location.code(), location.name());
        return LocationsStatus.builder()
          .code(location.code())
          .status(LocationsStatus.StatusEnum.SKIPPED)
          .build();
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
      return LocationsStatus.builder()
        .code(location.code())
        .status(LocationsStatus.StatusEnum.SUCCESS)
        .build();
    } catch (Exception e) {
      log.error("createShadowLocation:: Unexpected error creating shadow location: {} - {}",
        location.code(), location.name(), e);
      return LocationsStatus.builder()
        .code(location.code())
        .status(LocationsStatus.StatusEnum.ERROR)
        .cause(e.getMessage())
        .build();
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

  private @NotNull String formatAgencyQuery(String name, String code) {
    return String.format("(name==%s AND code==%s)", name, code);
  }
}
