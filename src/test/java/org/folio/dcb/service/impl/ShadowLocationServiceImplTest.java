package org.folio.dcb.service.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
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
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.List;
import java.util.UUID;
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
import org.folio.dcb.integration.invstorage.LocationUnitClient;
import org.folio.dcb.integration.invstorage.LocationsClient;
import org.folio.dcb.integration.invstorage.model.Location;
import org.folio.dcb.integration.invstorage.model.LocationUnit;
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
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;
import static org.mockito.ArgumentMatchers.eq;

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
  @Captor private ArgumentCaptor<Location> locCaptor;
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
      .extracting(Location::getCode, Location::getName, Location::isShadow)
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
      .extracting(Location::getCode, Location::getName, Location::isShadow)
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
      .extracting(Location::getCode, Location::getName, Location::isShadow)
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
        .addInstitutionsItem(refreshAgencyStatus(ERROR).cause("400 during [POST] to [/institutions] [POST]: []"))
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
        .addCampusesItem(refreshAgencyStatus(ERROR).cause("400 during [POST] to [/campuses] [POST]: []"))
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
        .addLibrariesItem(refreshAgencyStatus(ERROR).cause("400 during [POST] to [/libraries] [POST]: []"))));

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
      .addLocationsItem(refreshLocationStatus(ERROR).cause("400 during [POST] to [/locations] [POST]: []"))
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
  void createShadowLocations_negative_httpExceptionThrown() {
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenThrow(InternalServerError.class);

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

    @Test
  void createShadowLocations_positive_partialExistenceHierarchyReuse() {
    // TestMate-34ec793e0eaae755ccfec6a1494f9d1b
    // Given
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenReturn(servicePoint());
    when(locationUnitClient.findInstitutionsByQuery(agencySql(), true, 10, 0)).thenReturn(asSinglePage(institution()));
    when(locationUnitClient.findCampusesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findLibrariesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationsClient.findLocationByQuery(locationSql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.createCampus(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createLibrary(luCaptor.capture())).then(returningFirstArgument());
    when(locationsClient.createLocation(locCaptor.capture())).then(returningFirstArgument());
    var refreshRequest = refreshRequest(List.of(dcbLocation()), emptyList());
    // When
    var result = dcbHubLocationService.createShadowLocations(refreshRequest);
    // Then
    assertThat(result).isEqualTo(new RefreshShadowLocationResponse()
      .addLocationsItem(refreshLocationStatus(SUCCESS))
      .locationUnits(new RefreshLocationUnitsStatus()
        .addInstitutionsItem(refreshAgencyStatus(SKIPPED))
        .addCampusesItem(refreshAgencyStatus(SUCCESS))
        .addLibrariesItem(refreshAgencyStatus(SUCCESS))));
    verify(locationUnitClient, never()).createInstitution(any());
    verify(locationUnitClient).createCampus(any());
    verify(locationUnitClient).createLibrary(any());
    verify(locationsClient).createLocation(any());
    LocationUnit capturedCampus = luCaptor.getAllValues().stream()
      .filter(lu -> lu.getInstitutionId() != null && lu.getCampusId() == null)
      .findFirst()
      .orElseThrow();
    assertThat(capturedCampus.getInstitutionId()).isEqualTo(INSTITUTION_ID);
  }

    @Test
  void createShadowLocations_positive_combinedRequestGrouping() {
    // TestMate-94fcb8ae859525449cb6b22ac8ffa5f8
    // Given
    String loc1Name = "Location 1";
    String loc1Code = "LOC-1";
    String loc2Name = "Location 2";
    String loc2Code = "LOC-2";
    String loc1Sql = CqlQuery.exactMatchByNameAndCode(loc1Name, loc1Code).getQuery();
    String loc2Sql = CqlQuery.exactMatchByNameAndCode(loc2Name, loc2Code).getQuery();
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenReturn(servicePoint());
    when(locationUnitClient.findInstitutionsByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findCampusesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findLibrariesByQuery(agencySql(), true, 10, 0)).thenReturn(empty());
    when(locationsClient.findLocationByQuery(loc1Sql, true, 10, 0)).thenReturn(empty());
    when(locationsClient.findLocationByQuery(loc2Sql, true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.createInstitution(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createCampus(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createLibrary(luCaptor.capture())).then(returningFirstArgument());
    when(locationsClient.createLocation(locCaptor.capture())).then(returningFirstArgument());
    var agency = dcbAgency();
    var location1 = dcbLocation(loc1Name, loc1Code, agency);
    var location2 = dcbLocation(loc2Name, loc2Code, agency);
    var refreshRequest = refreshRequest(List.of(location1, location2), List.of());
    // When
    var result = dcbHubLocationService.createShadowLocations(refreshRequest);
    // Then
    assertThat(result).isEqualTo(new RefreshShadowLocationResponse()
      .addLocationsItem(refreshLocationStatus(SUCCESS).code(loc1Code))
      .addLocationsItem(refreshLocationStatus(SUCCESS).code(loc2Code))
      .locationUnits(new RefreshLocationUnitsStatus()
        .addInstitutionsItem(refreshAgencyStatus(SUCCESS))
        .addCampusesItem(refreshAgencyStatus(SUCCESS))
        .addLibrariesItem(refreshAgencyStatus(SUCCESS))));
    verify(locationUnitClient, times(1)).createInstitution(any());
    verify(locationUnitClient, times(1)).createCampus(any());
    verify(locationUnitClient, times(1)).createLibrary(any());
    verify(locationsClient, times(2)).createLocation(any());
    List<Location> capturedLocations = locCaptor.getAllValues();
    assertThat(capturedLocations)
      .extracting(Location::getCode, Location::getName)
      .containsExactlyInAnyOrder(
        org.assertj.core.groups.Tuple.tuple(loc1Code, loc1Name),
        org.assertj.core.groups.Tuple.tuple(loc2Code, loc2Name)
      );
    String sharedInstitutionId = capturedLocations.get(0).getInstitutionId();
    String sharedCampusId = capturedLocations.get(0).getCampusId();
    String sharedLibraryId = capturedLocations.get(0).getLibraryId();
    assertThat(capturedLocations.get(1).getInstitutionId()).isEqualTo(sharedInstitutionId);
    assertThat(capturedLocations.get(1).getCampusId()).isEqualTo(sharedCampusId);
    assertThat(capturedLocations.get(1).getLibraryId()).isEqualTo(sharedLibraryId);
  }

    @Test
  void createShadowLocations_positive_partialSuccessMultipleAgencies() {
    // TestMate-f3af2b8f3b8782a468074ae1dee25408
    // Given
    String agency1Name = "Agency Fail";
    String agency1Code = "AG-FAIL";
    String agency2Name = "Agency Success";
    String agency2Code = "AG-SUCCESS";
    String agency1Sql = CqlQuery.exactMatchByNameAndCode(agency1Name, agency1Code).getQuery();
    String agency2Sql = CqlQuery.exactMatchByNameAndCode(agency2Name, agency2Code).getQuery();
    when(dcbFeatureProperties.isFlexibleCirculationRulesEnabled()).thenReturn(true);
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenReturn(servicePoint());
    when(locationUnitClient.findInstitutionsByQuery(agency1Sql, true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.createInstitution(any())).thenAnswer(invocation -> {
      LocationUnit lu = invocation.getArgument(0);
      if (agency1Code.equals(lu.getCode())) {
        throw badRequestError("/institutions");
      }
      return lu;
    });
    when(locationUnitClient.findInstitutionsByQuery(agency2Sql, true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findCampusesByQuery(agency2Sql, true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.findLibrariesByQuery(agency2Sql, true, 10, 0)).thenReturn(empty());
    when(locationsClient.findLocationByQuery(agency2Sql, true, 10, 0)).thenReturn(empty());
    when(locationUnitClient.createCampus(luCaptor.capture())).then(returningFirstArgument());
    when(locationUnitClient.createLibrary(luCaptor.capture())).then(returningFirstArgument());
    when(locationsClient.createLocation(locCaptor.capture())).then(returningFirstArgument());
    var agency1 = new DcbAgency().name(agency1Name).code(agency1Code);
    var agency2 = new DcbAgency().name(agency2Name).code(agency2Code);
    var refreshRequest = refreshRequest(emptyList(), List.of(agency1, agency2));
    // When
    var result = dcbHubLocationService.createShadowLocations(refreshRequest);
    // Then
    assertThat(result.getLocationUnits().getInstitutions())
      .extracting(RefreshLocationStatus::getCode, RefreshLocationStatus::getStatus)
      .containsExactlyInAnyOrder(
        org.assertj.core.groups.Tuple.tuple(agency1Code, ERROR),
        org.assertj.core.groups.Tuple.tuple(agency2Code, SUCCESS)
      );
    assertThat(result.getLocationUnits().getCampuses())
      .extracting(RefreshLocationStatus::getCode, RefreshLocationStatus::getStatus)
      .containsExactlyInAnyOrder(
        org.assertj.core.groups.Tuple.tuple(agency1Code, SKIPPED),
        org.assertj.core.groups.Tuple.tuple(agency2Code, SUCCESS)
      );
    assertThat(result.getLocations())
      .extracting(RefreshLocationStatus::getCode, RefreshLocationStatus::getStatus)
      .containsExactlyInAnyOrder(
        org.assertj.core.groups.Tuple.tuple(agency1Code, SKIPPED),
        org.assertj.core.groups.Tuple.tuple(agency2Code, SUCCESS)
      );
    verify(locationUnitClient, times(2)).createInstitution(any());
    verify(locationUnitClient, times(1)).createCampus(any());
    verify(locationsClient, times(1)).createLocation(any());
    assertThat(locCaptor.getAllValues())
      .extracting(Location::getCode, Location::getName)
      .containsExactly(org.assertj.core.groups.Tuple.tuple(agency2Code, agency2Name));
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
    return CqlQuery.exactMatchByNameAndCode(AGENCY_NAME, AGENCY_CODE).getQuery();
  }

  private static String locationSql() {
    return CqlQuery.exactMatchByNameAndCode(LOCATION_NAME, LOCATION_CODE).getQuery();
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

  private static Location location() {
    var location = new Location();
    location.setId(LOCATION_ID);
    location.setCode(LOCATION_CODE);
    location.setName(LOCATION_NAME);
    location.setInstitutionId(INSTITUTION_ID);
    location.setCampusId(CAMPUS_ID);
    location.setLibraryId(LIBRARY_ID);
    return location;
  }

  private static HttpClientErrorException badRequestError(String url) {
    var statusText = "during [POST] to [%s] [POST]: []".formatted(url);
    return HttpClientErrorException.create(BAD_REQUEST, statusText, HttpHeaders.EMPTY, null, UTF_8);
  }
}
