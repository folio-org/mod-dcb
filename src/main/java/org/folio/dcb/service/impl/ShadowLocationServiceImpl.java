package org.folio.dcb.service.impl;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.folio.dcb.client.feign.LocationUnitClient.LocationUnit;
import static org.folio.dcb.utils.CqlQuery.exactMatchByNameAndCode;
import static org.folio.dcb.utils.DcbHubLocationsGroupingUtil.groupByAgency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.dcb.client.feign.LocationUnitClient;
import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.config.DcbFeatureProperties;
import org.folio.dcb.domain.dto.DcbLocation;
import org.folio.dcb.domain.dto.RefreshLocationStatus;
import org.folio.dcb.domain.dto.RefreshLocationStatusType;
import org.folio.dcb.domain.dto.RefreshLocationUnitsStatus;
import org.folio.dcb.domain.dto.RefreshShadowLocationResponse;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.domain.dto.ShadowLocationRefreshBody;
import org.folio.dcb.exception.ServiceException;
import org.folio.dcb.integration.dcb.model.AgencyKey;
import org.folio.dcb.integration.dcb.model.LocationAgenciesIds;
import org.folio.dcb.service.ShadowLocationService;
import org.folio.dcb.service.entities.DcbEntityServiceFacade;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class ShadowLocationServiceImpl implements ShadowLocationService {

  private static final String INACTIVE_FEATURE_MESSAGE =
    "Flexible circulation rules feature is disabled, cannot create shadow locations";

  private final LocationsClient locationsClient;
  private final LocationUnitClient locationUnitClient;
  private final DcbFeatureProperties dcbFeatureProperties;
  private final DcbEntityServiceFacade dcbEntityServiceFacade;

  public RefreshShadowLocationResponse createShadowLocations(ShadowLocationRefreshBody requestBody) {
    log.debug("createShadowLocations:: creating shadow locations");
    if (!dcbFeatureProperties.isFlexibleCirculationRulesEnabled()) {
      log.info("createShadowLocations:: {}", INACTIVE_FEATURE_MESSAGE);
      throw new ServiceException(INACTIVE_FEATURE_MESSAGE);
    }

    try {
      var servicePointRequest = dcbEntityServiceFacade.findOrCreateServicePoint();
      var locationList = prepareLocationsFromRequest(requestBody);
      if (CollectionUtils.isEmpty(locationList)) {
        log.debug("createShadowLocations:: No locations found in DCB Hub, skipping shadow location creation");
        return new RefreshShadowLocationResponse();
      }

      return createShadowLocations(servicePointRequest, locationList);
    } catch (Exception e) {
      log.error("createShadowLocations:: FeignException while fetching locations from DCB Hub", e);
      throw new ServiceException("Failed to create shadow locations", e);
    }
  }

  private RefreshShadowLocationResponse createShadowLocations(
    ServicePointRequest servicePointRequest, List<DcbLocation> locations) {

    var locationsGroupedByAgency = groupByAgency(locations);

    var locationUnitsResult = createLocationUnits(locationsGroupedByAgency.keySet());
    var locationStatuses = createShadowLocationEntries(locationsGroupedByAgency,
      locationUnitsResult.agencyLocationUnitMapping, servicePointRequest);

    var response = new RefreshShadowLocationResponse()
      .locationUnits(locationUnitsResult.locationUnits)
      .locations(locationStatuses);

    log.info("createShadowLocations:: shadow locations created: {}", locationStatuses.size());
    return response;
  }

  private LocationUnitsResult createLocationUnits(Set<AgencyKey> agencies) {
    RefreshLocationUnitsStatus locationUnits = new RefreshLocationUnitsStatus();
    Map<AgencyKey, LocationAgenciesIds> agencyLocationUnitMapping = new HashMap<>();

    agencies.forEach(agencyKey -> {
      log.debug("createLocationUnits:: Creating units for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());

      var institutionResult = createInstitution(agencyKey);
      locationUnits.addInstitutionsItem(institutionResult.locationUnitsStatus);

      var campusResult = createCampus(agencyKey, institutionResult.institution);
      locationUnits.addCampusesItem(campusResult.locationUnitsStatus);

      var libraryResult = createLibrary(agencyKey, campusResult.campus);
      locationUnits.addLibrariesItem(libraryResult.locationUnitsStatus);

      agencyLocationUnitMapping.put(agencyKey, new LocationAgenciesIds(
        getLocationUnitId(institutionResult.institution()),
        getLocationUnitId(campusResult.campus()),
        getLocationUnitId(libraryResult.library()))
      );
    });

    return new LocationUnitsResult(locationUnits, agencyLocationUnitMapping);
  }

  private InstitutionResult createInstitution(AgencyKey agencyKey) {
    try {
      var query = exactMatchByNameAndCode(agencyKey.agencyName(), agencyKey.agencyCode());
      var locationUnitResultList = locationUnitClient.findInstitutionsByQuery(query, true, 10, 0);

      if (CollectionUtils.isNotEmpty(locationUnitResultList.getResult())) {
        log.info("createInstitution:: Institution already exists for agency: {} - {}",
          agencyKey.agencyCode(), agencyKey.agencyName());

        return new InstitutionResult(
          locationUnitResultList.getResult().getFirst(),
          RefreshLocationStatus.builder()
            .code(agencyKey.agencyCode())
            .status(RefreshLocationStatusType.SKIPPED)
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
        RefreshLocationStatus.builder()
          .code(agencyKey.agencyCode())
          .status(RefreshLocationStatusType.SUCCESS)
          .build());
    } catch (Exception e) {
      log.error("createInstitution:: Error creating institution for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName(), e);

      return new InstitutionResult(
        null,
        RefreshLocationStatus.builder()
          .code(agencyKey.agencyCode())
          .status(RefreshLocationStatusType.ERROR)
          .cause(e.getMessage())
          .build());
    }
  }

  private CampusResult createCampus(AgencyKey agencyKey, LocationUnit institution) {
    try {
      if (institution == null) {
        log.error("createCampus:: Institution is null for agency: {} - {}, cannot create campus",
          agencyKey.agencyCode(), agencyKey.agencyName());

        return new CampusResult(
          null,
          RefreshLocationStatus.builder()
            .code(agencyKey.agencyCode())
            .status(RefreshLocationStatusType.SKIPPED)
            .cause("Parent institution is not created")
            .build());
      }

      var searchQuery = exactMatchByNameAndCode(agencyKey.agencyName(), agencyKey.agencyCode());
      var locationUnitResultList = locationUnitClient.findCampusesByQuery(searchQuery, true, 10, 0);

      if (!locationUnitResultList.getResult().isEmpty()) {
        log.info("createCampus:: campus already exists for agency: {} - {}",
          agencyKey.agencyCode(), agencyKey.agencyName());

        return new CampusResult(
          locationUnitResultList.getResult().getFirst(),
          RefreshLocationStatus.builder()
            .code(agencyKey.agencyCode())
            .status(RefreshLocationStatusType.SKIPPED)
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
        RefreshLocationStatus.builder()
          .code(agencyKey.agencyCode())
          .status(RefreshLocationStatusType.SUCCESS)
          .build());
    } catch (Exception e) {
      log.error("createCampus:: Error creating campus for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName(), e);

      return new CampusResult(
        null,
        RefreshLocationStatus.builder()
          .code(agencyKey.agencyCode())
          .status(RefreshLocationStatusType.ERROR)
          .cause(e.getMessage())
          .build());
    }
  }

  private LibraryResult createLibrary(AgencyKey agencyKey, LocationUnit campus) {
    try {
      if (campus == null) {
        log.error("createLibrary:: Campus is null for agency: {} - {}, cannot create library",
          agencyKey.agencyCode(), agencyKey.agencyName());

        return new LibraryResult(
          null,
          RefreshLocationStatus.builder()
            .code(agencyKey.agencyCode())
            .status(RefreshLocationStatusType.SKIPPED)
            .cause("Parent campus is not created")
            .build());
      }

      var query = exactMatchByNameAndCode(agencyKey.agencyName(), agencyKey.agencyCode());
      var locationUnitResultList = locationUnitClient.findLibrariesByQuery(query, true, 10, 0);

      if (CollectionUtils.isNotEmpty(locationUnitResultList.getResult())) {
        log.info("createLibrary:: library already exists for agency: {} - {}",
          agencyKey.agencyCode(), agencyKey.agencyName());

        return new LibraryResult(
          locationUnitResultList.getResult().getFirst(),
          RefreshLocationStatus.builder()
            .code(agencyKey.agencyCode())
            .status(RefreshLocationStatusType.SKIPPED)
            .build());
      }

      var library = LocationUnit.builder()
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
        RefreshLocationStatus.builder()
          .code(agencyKey.agencyCode())
          .status(RefreshLocationStatusType.SUCCESS)
          .build());
    } catch (Exception e) {
      log.error("createLibrary:: Error creating library for agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName(), e);

      return new LibraryResult(
        null,
        RefreshLocationStatus.builder()
          .code(agencyKey.agencyCode())
          .status(RefreshLocationStatusType.ERROR)
          .cause(e.getMessage())
          .build());
    }
  }

  private List<RefreshLocationStatus> createShadowLocationEntries(
    Map<AgencyKey, List<DcbLocation>> locationsGroupedByAgency,
    Map<AgencyKey, LocationAgenciesIds> agencyLocationUnitMapping,
    ServicePointRequest servicePointRequest) {

    var locationStatuses = new ArrayList<RefreshLocationStatus>();
    locationsGroupedByAgency.forEach((agencyKey, locationCodeNamePairs) -> {
      log.debug("createShadowLocationEntries:: Processing agency: {} - {}",
        agencyKey.agencyCode(), agencyKey.agencyName());

      var locationAgenciesIds = agencyLocationUnitMapping.get(agencyKey);
      locationCodeNamePairs.forEach(location -> {
        RefreshLocationStatus status = createShadowLocation(location, locationAgenciesIds, servicePointRequest);
        locationStatuses.add(status);
      });
    });

    return locationStatuses;
  }

  private RefreshLocationStatus createShadowLocation(DcbLocation location,
    LocationAgenciesIds locationAgenciesIds, ServicePointRequest servicePointRequest) {
    var locationName = location.getName();
    var locationCode = location.getCode();
    try {
      if (anyNull(locationAgenciesIds.institutionId(), locationAgenciesIds.libraryId(), locationAgenciesIds.campusId())) {
        log.error(
          "createShadowLocation:: Location agencies IDs are incomplete or null for location: {} - {}, cannot create shadow location. locationAgenciesIds are: {}",
          locationCode, locationName, locationAgenciesIds.toString());
        return RefreshLocationStatus.builder()
          .code(locationCode)
          .status(RefreshLocationStatusType.SKIPPED)
          .cause("Parent location units not created")
          .build();
      }

      var searchQuery = exactMatchByNameAndCode(locationName, locationCode);
      var locationDTOResultList = locationsClient.findLocationByQuery(searchQuery, true, 10, 0);
      if (!locationDTOResultList.getResult().isEmpty()) {
        log.info("createShadowLocation:: Location already exists: {} - {}, skipping...",
          locationCode, locationName);
        return RefreshLocationStatus.builder()
          .code(locationCode)
          .status(RefreshLocationStatusType.SKIPPED)
          .build();
      }

      log.debug("createShadowLocation:: Creating shadow location: {} - {}",
        locationCode, locationName);

      var shadowLocation = LocationsClient.LocationDTO.builder()
        .id(UUID.randomUUID().toString())
        .code(locationCode)
        .name(locationName)
        .institutionId(locationAgenciesIds.institutionId())
        .campusId(locationAgenciesIds.campusId())
        .libraryId(locationAgenciesIds.libraryId())
        .primaryServicePoint(servicePointRequest.getId())
        .servicePointIds(singletonList(servicePointRequest.getId()))
        .isShadow(true)
        .build();

      locationsClient.createLocation(shadowLocation);
      log.debug("createShadowLocation:: Created shadow location: {} - {}",
        shadowLocation.getCode(), shadowLocation.getName());
      return RefreshLocationStatus.builder()
        .code(locationCode)
        .status(RefreshLocationStatusType.SUCCESS)
        .build();
    } catch (Exception e) {
      log.error("createShadowLocation:: Unexpected error creating shadow location: {} - {}",
        locationCode, locationName, e);
      return RefreshLocationStatus.builder()
        .code(locationCode)
        .status(RefreshLocationStatusType.ERROR)
        .cause(e.getMessage())
        .build();
    }
  }

  private static List<DcbLocation> prepareLocationsFromRequest(ShadowLocationRefreshBody requestBody) {
    var agencies = requestBody.getAgencies();
    var locationsFromAgencies = agencies.stream()
      .map(agency -> new DcbLocation()
        .agency(agency)
        .code(agency.getCode())
        .name(agency.getName()))
      .toList();

    var allLocations = new ArrayList<>(requestBody.getLocations());
    allLocations.addAll(locationsFromAgencies);

    return allLocations;
  }

  private static String getLocationUnitId(LocationUnit institutionResult) {
    return Optional.ofNullable(institutionResult)
      .map(LocationUnit::getId)
      .orElse(null);
  }

  private record LocationUnitsResult(
    RefreshLocationUnitsStatus locationUnits,
    Map<AgencyKey, LocationAgenciesIds> agencyLocationUnitMapping) {}

  private record InstitutionResult(LocationUnit institution, RefreshLocationStatus locationUnitsStatus) {}

  private record CampusResult(LocationUnit campus, RefreshLocationStatus locationUnitsStatus) {}

  private record LibraryResult(LocationUnit library, RefreshLocationStatus locationUnitsStatus) {}
}
