package org.folio.dcb.service.impl;

import static feign.Request.HttpMethod.POST;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.folio.dcb.domain.ResultList.asSinglePage;
import static org.folio.dcb.domain.ResultList.empty;
import static org.folio.dcb.domain.dto.RefreshLocationStatusType.ERROR;
import static org.folio.dcb.domain.dto.RefreshLocationStatusType.SKIPPED;
import static org.folio.dcb.domain.dto.RefreshLocationStatusType.SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.Response;
import java.util.List;
import java.util.UUID;
import org.folio.dcb.client.feign.LocationUnitClient;
import org.folio.dcb.client.feign.LocationUnitClient.LocationUnit;
import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.client.feign.LocationsClient.LocationDTO;
import org.folio.dcb.config.DcbFeatureProperties;
import org.folio.dcb.domain.dto.DcbAgency;
import org.folio.dcb.domain.dto.DcbLocation;
import org.folio.dcb.domain.dto.RefreshLocationStatus;
import org.folio.dcb.domain.dto.RefreshLocationStatusType;
import org.folio.dcb.domain.dto.RefreshLocationUnitsStatus;
import org.folio.dcb.domain.dto.RefreshShadowLocationResponse;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.domain.dto.ShadowLocationRefreshBody;
import org.folio.dcb.exception.ServiceException;
import org.folio.dcb.service.entities.DcbEntityServiceFacade;
import org.folio.dcb.utils.CqlQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class ShadowLocationServiceImplTest {

  private static final String LOCATION_ID = UUID.randomUUID().toString();
  private static final String INSTITUTION_ID = UUID.randomUUID().toString();
  private static final String CAMPUS_ID = UUID.randomUUID().toString();
  private static final String LIBRARY_ID = UUID.randomUUID().toString();

  private static final String LOCATION_NAME = "Test Location";
  private static final String LOCATION_CODE = "LOC-TEST";
  private static final String AGENCY_CODE = "AG-TEST";
  private static final String AGENCY_NAME = "Test Agency";

  @InjectMocks private ShadowLocationServiceImpl dcbHubLocationService;
  @Mock private LocationsClient locationsClient;
  @Mock private LocationUnitClient locationUnitClient;
  @Mock private DcbFeatureProperties dcbFeatureProperties;
  @Mock private DcbEntityServiceFacade dcbEntityServiceFacade;
  @Captor private ArgumentCaptor<LocationDTO> locCaptor;
  @Captor private ArgumentCaptor<LocationUnit> luCaptor;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(locationsClient, locationsClient, dcbEntityServiceFacade);
  }

  @Test
  void createShadowLocations_positive_allNewEntities() {
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenReturn(servicePoint());
    when(locationUnitClient.findInstitutionsByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findCampusesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findLibrariesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationsClient.findLocationByQuery(locationSql(), true, 10, 0)).thenReturn(empty());

    when(locationUnitClient.createInstitution(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createCampus(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createLibrary(luCaptor.capture())).then(returningFirstArgument());
    when(locationsClient.createLocation(locCaptor.capture())).then(returningFirstArgument());

    var refreshRequest = refreshRequest(List.of(dcbLocation()), emptyList());
    var result = dcbHubLocationService.createShadowLocations(refreshRequest);

    assertThat(result).isEqualTo(new RefreshShadowLocationResponse()
      .addLocationsItem(refreshLocationStatus(SUCCESS))
      .locationUnits(new RefreshLocationUnitsStatus()
        .addInstitutionsItem(refreshAgencyStatus(SUCCESS))
        .addLibrariesItem(refreshAgencyStatus(SUCCESS))
        .addCampusesItem(refreshAgencyStatus(SUCCESS))));

    assertThat(luCaptor.getAllValues())
      .extracting(LocationUnit::getName, LocationUnit::getCode, LocationUnit::isShadow)
      .containsExactly(
        tuple(AGENCY_NAME, AGENCY_CODE, true),
        tuple(AGENCY_NAME, AGENCY_CODE, true),
        tuple(AGENCY_NAME, AGENCY_CODE, true));

    assertThat(locCaptor.getAllValues())
      .extracting(LocationDTO::getCode, LocationDTO::getName, LocationDTO::isShadow)
      .containsExactly(tuple(LOCATION_CODE, LOCATION_NAME, true));

    verify(locationUnitClient).createInstitution(any());
    verify(locationUnitClient).createCampus(any());
    verify(locationUnitClient).createLibrary(any());
    verify(locationsClient).createLocation(any());
  }

  @Test
  void createShadowLocations_positive_allNewEntitiesWhenOnlyAgencyProvided() {
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenReturn(servicePoint());
    when(locationUnitClient.findInstitutionsByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findCampusesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findLibrariesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationsClient.findLocationByQuery(agencySql(), true, 10, 0)).thenReturn(empty());

    when(locationUnitClient.createInstitution(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createCampus(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createLibrary(luCaptor.capture())).then(returningFirstArgument());
    when(locationsClient.createLocation(locCaptor.capture())).then(returningFirstArgument());

    var refreshRequest = refreshRequest(emptyList(), List.of(dcbAgency()));
    var result = dcbHubLocationService.createShadowLocations(refreshRequest);

    assertThat(result).isEqualTo(new RefreshShadowLocationResponse()
      .addLocationsItem(refreshAgencyStatus(SUCCESS))
      .locationUnits(new RefreshLocationUnitsStatus()
        .addInstitutionsItem(refreshAgencyStatus(SUCCESS))
        .addLibrariesItem(refreshAgencyStatus(SUCCESS))
        .addCampusesItem(refreshAgencyStatus(SUCCESS))));

    assertThat(luCaptor.getAllValues())
      .extracting(LocationUnit::getCode, LocationUnit::getName, LocationUnit::isShadow)
      .containsExactly(
        tuple(AGENCY_CODE, AGENCY_NAME, true),
        tuple(AGENCY_CODE, AGENCY_NAME, true),
        tuple(AGENCY_CODE, AGENCY_NAME, true));

    assertThat(locCaptor.getAllValues())
      .extracting(LocationDTO::getCode, LocationDTO::getName, LocationDTO::isShadow)
      .containsExactly(tuple(AGENCY_CODE, AGENCY_NAME, true));

    verify(locationUnitClient).createInstitution(any());
    verify(locationUnitClient).createCampus(any());
    verify(locationUnitClient).createLibrary(any());
    verify(locationsClient).createLocation(any());
  }

  @Test
  void createShadowLocations_positive_newEntitiesForCombinedRequest() {
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenReturn(servicePoint());
    when(locationUnitClient.findInstitutionsByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findCampusesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findLibrariesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationsClient.findLocationByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationsClient.findLocationByQuery(locationSql(), true, 10, 0)).thenReturn(empty());

    when(locationUnitClient.createInstitution(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createCampus(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createLibrary(luCaptor.capture())).then(returningFirstArgument());
    when(locationsClient.createLocation(locCaptor.capture())).then(returningFirstArgument());

    var refreshRequest = refreshRequest(List.of(dcbLocation()), List.of(dcbAgency()));
    var result = dcbHubLocationService.createShadowLocations(refreshRequest);

    assertThat(result).isEqualTo(new RefreshShadowLocationResponse()
      .addLocationsItem(refreshLocationStatus(SUCCESS))
      .addLocationsItem(refreshAgencyStatus(SUCCESS))
      .locationUnits(new RefreshLocationUnitsStatus()
        .addInstitutionsItem(refreshAgencyStatus(SUCCESS))
        .addLibrariesItem(refreshAgencyStatus(SUCCESS))
        .addCampusesItem(refreshAgencyStatus(SUCCESS))));

    assertThat(luCaptor.getAllValues())
      .extracting(LocationUnit::getCode, LocationUnit::getName, LocationUnit::isShadow)
      .containsExactly(
        tuple(AGENCY_CODE, AGENCY_NAME, true),
        tuple(AGENCY_CODE, AGENCY_NAME, true),
        tuple(AGENCY_CODE, AGENCY_NAME, true));

    assertThat(locCaptor.getAllValues())
      .extracting(LocationDTO::getCode, LocationDTO::getName, LocationDTO::isShadow)
      .containsExactly(
        tuple(LOCATION_CODE, LOCATION_NAME, true),
        tuple(AGENCY_CODE, AGENCY_NAME, true));

    verify(locationUnitClient).createInstitution(any());
    verify(locationUnitClient).createCampus(any());
    verify(locationUnitClient).createLibrary(any());
    verify(locationsClient, times(2)).createLocation(any());
  }

  @Test
  void createShadowLocations_positive_allEntitiesExist() {
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenReturn(servicePoint());
    when(locationUnitClient.findInstitutionsByQuery(agencySql(), true, 10, 0)).thenReturn(asSinglePage(institution()));
    when(locationUnitClient.findCampusesByQuery(agencySql(), true, 10, 0)).thenReturn(asSinglePage(campus()));
    when(locationUnitClient.findLibrariesByQuery(agencySql(), true, 10, 0)).thenReturn(asSinglePage(library()));
    when(locationsClient.findLocationByQuery(locationSql(), true, 10, 0)).thenReturn(asSinglePage(location()));

    var refreshRequest = refreshRequest(List.of(dcbLocation()), emptyList());
    var result = dcbHubLocationService.createShadowLocations(refreshRequest);

    assertThat(result).isEqualTo(new RefreshShadowLocationResponse()
      .addLocationsItem(refreshLocationStatus(SKIPPED))
      .locationUnits(new RefreshLocationUnitsStatus()
        .addInstitutionsItem(refreshAgencyStatus(SKIPPED))
        .addCampusesItem(refreshAgencyStatus(SKIPPED))
        .addLibrariesItem(refreshAgencyStatus(SKIPPED))));

    verify(locationUnitClient, never()).createInstitution(any());
    verify(locationUnitClient, never()).createCampus(any());
    verify(locationUnitClient, never()).createLibrary(any());
    verify(locationsClient, never()).createLocation(any());
  }

  @Test
  void createShadowLocations_positive_failedToCreateInstitution() {
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenReturn(servicePoint());
    when(locationUnitClient.findInstitutionsByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.createInstitution(luCaptor.capture())).thenThrow(badRequestError("/institutions"));

    var refreshRequest = refreshRequest(List.of(dcbLocation()), emptyList());
    var result = dcbHubLocationService.createShadowLocations(refreshRequest);

    assertThat(result).isEqualTo(new RefreshShadowLocationResponse()
      .addLocationsItem(refreshLocationStatus(SKIPPED).cause("Parent location units not created"))
      .locationUnits(new RefreshLocationUnitsStatus()
        .addInstitutionsItem(refreshAgencyStatus(ERROR).cause("[400] during [POST] to [/institutions] [POST]: []"))
        .addCampusesItem(refreshAgencyStatus(SKIPPED).cause("Parent institution is not created"))
        .addLibrariesItem(refreshAgencyStatus(SKIPPED).cause("Parent campus is not created"))));

    verify(locationUnitClient, never()).createCampus(any());
    verify(locationUnitClient, never()).createLibrary(any());
    verify(locationsClient, never()).createLocation(any());
  }

  @Test
  void createShadowLocations_positive_failedToCreateCampus() {
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenReturn(servicePoint());
    when(locationUnitClient.findInstitutionsByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findCampusesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.createInstitution(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createCampus(luCaptor.capture())).thenThrow(badRequestError("/campuses"));

    var refreshRequest = refreshRequest(List.of(dcbLocation()), emptyList());
    var result = dcbHubLocationService.createShadowLocations(refreshRequest);

    assertThat(result).isEqualTo(new RefreshShadowLocationResponse()
      .addLocationsItem(refreshLocationStatus(SKIPPED).cause("Parent location units not created"))
      .locationUnits(new RefreshLocationUnitsStatus()
        .addInstitutionsItem(refreshAgencyStatus(SUCCESS))
        .addCampusesItem(refreshAgencyStatus(ERROR).cause("[400] during [POST] to [/campuses] [POST]: []"))
        .addLibrariesItem(refreshAgencyStatus(SKIPPED).cause("Parent campus is not created"))));

    verify(locationUnitClient, never()).createLibrary(any());
    verify(locationsClient, never()).createLocation(any());
  }

  @Test
  void createShadowLocations_positive_failedToCreateLibrary() {
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenReturn(servicePoint());
    when(locationUnitClient.findInstitutionsByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findCampusesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findLibrariesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());

    when(locationUnitClient.createInstitution(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createCampus(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createLibrary(luCaptor.capture())).thenThrow(badRequestError("/libraries"));

    var refreshRequest = refreshRequest(List.of(dcbLocation()), emptyList());
    var result = dcbHubLocationService.createShadowLocations(refreshRequest);

    assertThat(result).isEqualTo(new RefreshShadowLocationResponse()
      .addLocationsItem(refreshLocationStatus(SKIPPED).cause("Parent location units not created"))
      .locationUnits(new RefreshLocationUnitsStatus()
        .addInstitutionsItem(refreshAgencyStatus(SUCCESS))
        .addCampusesItem(refreshAgencyStatus(SUCCESS))
        .addLibrariesItem(refreshAgencyStatus(ERROR).cause("[400] during [POST] to [/libraries] [POST]: []"))));

    verify(locationsClient, never()).createLocation(any());
  }

  @Test
  void createShadowLocations_positive_failedToCreateLocation() {
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenReturn(servicePoint());
    when(locationUnitClient.findInstitutionsByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findCampusesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findLibrariesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationsClient.findLocationByQuery(locationSql(), true, 10, 0)).thenReturn(empty());

    when(locationUnitClient.createInstitution(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createCampus(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createLibrary(luCaptor.capture())).then(returningFirstArgument());
    when(locationsClient.createLocation(locCaptor.capture())).thenThrow(badRequestError("/locations"));

    var refreshRequest = refreshRequest(List.of(dcbLocation()), emptyList());
    var result = dcbHubLocationService.createShadowLocations(refreshRequest);

    assertThat(result).isEqualTo(new RefreshShadowLocationResponse()
      .addLocationsItem(refreshLocationStatus(ERROR).cause("[400] during [POST] to [/locations] [POST]: []"))
      .locationUnits(new RefreshLocationUnitsStatus()
        .addInstitutionsItem(refreshAgencyStatus(SUCCESS))
        .addCampusesItem(refreshAgencyStatus(SUCCESS))
        .addLibrariesItem(refreshAgencyStatus(SUCCESS))));

    verify(locationUnitClient).createInstitution(any());
    verify(locationUnitClient).createCampus(any());
    verify(locationUnitClient).createLibrary(any());
    verify(locationsClient).createLocation(any());
  }

  @Test
  void createShadowLocations_positive_emptyRefreshRequest() {
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenReturn(servicePoint());
    var response = dcbHubLocationService.createShadowLocations(refreshRequest(emptyList(), emptyList()));

    assertThat(response).isEqualTo(new RefreshShadowLocationResponse());
    verify(locationUnitClient, never()).createInstitution(any());
    verify(locationUnitClient, never()).createCampus(any());
    verify(locationUnitClient, never()).createLibrary(any());
    verify(locationsClient, never()).createLocation(any());
  }

  @Test
  void createShadowLocations_negative_featureIsDisable() {
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(false);

    var refreshRequest = refreshRequest(emptyList(), emptyList());
    assertThatThrownBy(() -> dcbHubLocationService.createShadowLocations(refreshRequest))
      .isInstanceOf(ServiceException.class)
      .hasMessage("Flexible circulation rules feature is disabled, cannot create shadow locations");
  }

  @Test
  void createShadowLocations_negative_feignExceptionThrown() {
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenThrow(FeignException.class);

    var location = dcbLocation("test", "test", dcbAgency("agency", "AG-1"));
    var refreshRequest = refreshRequest(List.of(location), emptyList());
    assertThatThrownBy(() -> dcbHubLocationService.createShadowLocations(refreshRequest))
      .isInstanceOf(ServiceException.class)
      .hasMessage("Failed to create shadow locations");
  }

  @Test
  void createShadowLocations_negative_serviceExceptionThrown() {
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenThrow(ServiceException.class);

    var location = dcbLocation("test", "test", dcbAgency("agency", "AG-1"));
    var refreshRequest = refreshRequest(List.of(location), emptyList());
    assertThatThrownBy(() -> dcbHubLocationService.createShadowLocations(refreshRequest))
      .isInstanceOf(ServiceException.class)
      .hasMessage("Failed to create shadow locations");
  }

  private static ShadowLocationRefreshBody refreshRequest(List<DcbLocation> locations, List<DcbAgency> agencies) {
    return new ShadowLocationRefreshBody()
      .locations(locations)
      .agencies(agencies);
  }

  private static DcbLocation dcbLocation() {
    return dcbLocation(LOCATION_NAME, LOCATION_CODE, dcbAgency());
  }

  private static DcbLocation dcbLocation(String name, String code, DcbAgency agency) {
    return new DcbLocation().name(name).code(code).agency(agency);
  }

  private static DcbAgency dcbAgency() {
    return dcbAgency(AGENCY_NAME, AGENCY_CODE);
  }

  private static DcbAgency dcbAgency(String name, String code) {
    return new DcbAgency().name(name).code(code);
  }

  private static ServicePointRequest servicePoint() {
    return new ServicePointRequest().id("test-service-point");
  }

  private static String agencySql() {
    return CqlQuery.exactMatchByNameAndCode(AGENCY_NAME, AGENCY_CODE);
  }

  private static String locationSql() {
    return CqlQuery.exactMatchByNameAndCode(LOCATION_NAME, LOCATION_CODE);
  }

  private static <T> Answer<T> returningFirstArgument() {
    return inv -> inv.getArgument(0);
  }

  private static RefreshLocationStatus refreshAgencyStatus(RefreshLocationStatusType status) {
    return new RefreshLocationStatus().code(AGENCY_CODE).status(status);
  }

  private static RefreshLocationStatus refreshLocationStatus(RefreshLocationStatusType status) {
    return new RefreshLocationStatus().code(LOCATION_CODE).status(status);
  }

  private static LocationUnit institution() {
    var locationUnit = new LocationUnit();
    locationUnit.setId(INSTITUTION_ID);
    locationUnit.setCode(AGENCY_CODE);
    locationUnit.setName(AGENCY_NAME);
    return locationUnit;
  }

  private static LocationUnit library() {
    var locationUnit = new LocationUnit();
    locationUnit.setId(LIBRARY_ID);
    locationUnit.setCode(AGENCY_CODE);
    locationUnit.setName(AGENCY_NAME);
    locationUnit.setCampusId(CAMPUS_ID);
    locationUnit.setInstitutionId(INSTITUTION_ID);
    return locationUnit;
  }

  private static LocationUnit campus() {
    var locationUnit = new LocationUnit();
    locationUnit.setId(CAMPUS_ID);
    locationUnit.setCode(AGENCY_CODE);
    locationUnit.setName(AGENCY_NAME);
    locationUnit.setInstitutionId(INSTITUTION_ID);
    return locationUnit;
  }

  private static LocationDTO location() {
    var location = new LocationDTO();
    location.setId(LOCATION_ID);
    location.setCode(LOCATION_CODE);
    location.setName(LOCATION_NAME);
    location.setInstitutionId(INSTITUTION_ID);
    location.setCampusId(CAMPUS_ID);
    location.setLibraryId(LIBRARY_ID);
    return location;
  }

  private static FeignException badRequestError(String url) {
    var request = Response.builder()
      .status(400)
      .request(Request.create(POST, url, emptyMap(), null, null, null))
      .build();
    return FeignException.errorStatus("POST", request);
  }
}
