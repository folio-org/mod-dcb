package org.folio.dcb.integration.dcb;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.dcb.client.feign.LocationUnitClient;
import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.exception.DcbHubLocationException;
import org.folio.dcb.integration.dcb.config.DcbHubProperties;
import org.folio.dcb.integration.dcb.model.DcbAgency;
import org.folio.dcb.integration.dcb.model.DcbHubLocationResponse;
import org.folio.dcb.integration.dcb.model.DcbLocation;
import org.folio.dcb.integration.keycloak.DcbHubKCCredentialSecureStore;
import org.folio.dcb.integration.keycloak.DcbHubKCTokenService;
import org.folio.dcb.integration.keycloak.model.DcbHubKCCredentials;
import org.folio.dcb.service.entities.DcbEntityServiceFacade;
import org.folio.dcb.utils.CqlQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DcbHubLocationServiceImplTest {

  @Mock
  private DcbHubKCTokenService dcbHubKCTokenService;

  @Mock
  private DcbHubKCCredentialSecureStore dcbHubKCCredentialSecureStore;

  @Mock
  private DcbHubLocationClient dcbHubLocationClient;

  @Mock
  private LocationsClient locationsClient;

  @Mock
  private LocationUnitClient locationUnitClient;

  @Mock
  private DcbEntityServiceFacade dcbEntityServiceFacade;

  @InjectMocks
  private DcbHubLocationServiceImpl dcbHubLocationService;

  private static final String BEARER_TOKEN = "test-token";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(dcbHubLocationService, "batchSize", 5);
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(true);
    ReflectionTestUtils.setField(dcbHubLocationService, "dcbHubProperties", dcbHubProperties);

    DcbHubKCCredentials credentials = new DcbHubKCCredentials();
    credentials.setClientId("test-client");
    credentials.setClientSecret("test-secret");

    ServicePointRequest servicePointRequest = new ServicePointRequest();
    servicePointRequest.setId("sp-123");
    when(dcbEntityServiceFacade.findOrCreateServicePoint()).thenReturn(servicePointRequest);
    when(dcbHubKCCredentialSecureStore.getDcbHubKCCredentials()).thenReturn(credentials);
    when(dcbHubKCTokenService.getBearerAccessToken(any())).thenReturn(BEARER_TOKEN);
  }

  @Test
  void createShadowLocations_SinglePage_AgencyAndLocationNotExistAlready_Success() {
    // Given
    DcbLocation location1 = createTestLocation("Location 1", "loc1", "Agency Name 1", "agencyCode1");
    DcbLocation location2 = createTestLocation("Location 2", "loc2", "Agency Name 1", "agencyCode1");

    DcbHubLocationResponse response = new DcbHubLocationResponse();
    response.setContent(Arrays.asList(location1, location2));
    response.setTotalSize(2);
    response.setTotalPages(1);

    // Mocking
    when(dcbHubLocationClient.getLocations(1, 5, BEARER_TOKEN))
      .thenReturn(response);

    mockEmptyLocationUnitResponses("Agency Name 1", "agencyCode1");
    mockEmptyLocationDTOResponses("Location 1", "loc1");
    mockEmptyLocationDTOResponses("Location 2", "loc2");

    // When
    dcbHubLocationService.createShadowLocations(false);

    // Then
    verify(dcbHubLocationClient).getLocations(1, 5, BEARER_TOKEN);
    verify(locationUnitClient).createInstitution(any());
    verify(locationUnitClient).createCampus(any());
    verify(locationUnitClient).createLibrary(any());
    verify(locationsClient, times(2)).createLocation(any());
  }

  @Test
  void createShadowLocations_SinglePage_AgencyAndLocationExistAlready_Success1() {
    // Given
    DcbLocation location1 = createTestLocation( "Location 1", "loc1",  "Agency Name 1", "agencyCode1");
    DcbLocation location2 = createTestLocation("Location 2", "loc2",  "Agency Name 1", "agencyCode1");

    DcbHubLocationResponse response = new DcbHubLocationResponse();
    response.setContent(Arrays.asList(location1, location2));
    response.setTotalSize(2);
    response.setTotalPages(1);

    // Mocking
    when(dcbHubLocationClient.getLocations(1, 5, BEARER_TOKEN))
      .thenReturn(response);

    mockLocationUnitResponses("Agency Name 1", "agencyCode1");
    mockLocationDTOResponses("Location 1", "loc1");
    mockLocationDTOResponses("Location 2", "loc2");

    // When
    dcbHubLocationService.createShadowLocations(false);

    // Then
    verify(dcbHubLocationClient).getLocations(1, 5, BEARER_TOKEN);
    verify(locationUnitClient, never()).createInstitution(any());
    verify(locationUnitClient).findInstitutionsByQuery(anyString(), anyBoolean(), anyInt(), anyInt());
    verify(locationUnitClient, never()).createCampus(any());
    verify(locationUnitClient).findCampusesByQuery(anyString(), anyBoolean(), anyInt(), anyInt());
    verify(locationUnitClient, never()).createLibrary(any());
    verify(locationUnitClient).findLibrariesByQuery(anyString(), anyBoolean(), anyInt(), anyInt());
    verify(locationsClient, never()).createLocation(any());
    verify(locationsClient, times(2)).findLocationByQuery(anyString(), any(Boolean.class), anyInt(), anyInt());
  }

  @Test
  void createShadowLocations_MultiplePages_Success() {
    // Given
    DcbLocation location1 = createTestLocation( "Location 1", "loc1", "Agency Name 1", "agencyCode1");
    DcbLocation location2 = createTestLocation("Location 2", "loc2", "Agency Name 2", "agencyCode2");

    DcbHubLocationResponse page1 = new DcbHubLocationResponse();
    page1.setContent(Collections.singletonList(location1));
    page1.setTotalSize(2);
    page1.setTotalPages(2);

    DcbHubLocationResponse page2 = new DcbHubLocationResponse();
    page2.setContent(Collections.singletonList(location2));
    page2.setTotalSize(2);
    page2.setTotalPages(2);

    // Mocking
    when(dcbHubLocationClient.getLocations(1, 5, BEARER_TOKEN))
      .thenReturn(page1);
    when(dcbHubLocationClient.getLocations(2, 5, BEARER_TOKEN))
      .thenReturn(page2);

    mockEmptyLocationUnitResponses("Agency Name 1", "agencyCode1");
    mockEmptyLocationUnitResponses("Agency Name 2", "agencyCode2");
    mockEmptyLocationDTOResponses( "Location 1", "loc1");
    mockEmptyLocationDTOResponses("Location 2", "loc2");

    // When
    dcbHubLocationService.createShadowLocations(false);

    // Then
    verify(dcbHubLocationClient).getLocations(1, 5, BEARER_TOKEN);
    verify(dcbHubLocationClient).getLocations(2, 5, BEARER_TOKEN);

    validateInstitutions();
    validateCampus();
    validateLibrary();
    validateLocations();
  }

  private void validateInstitutions() {
    ArgumentCaptor<LocationUnitClient.LocationUnit> createInstitutionCaptor = ArgumentCaptor.forClass(LocationUnitClient.LocationUnit.class);
    // Verify institution creation
    verify(locationUnitClient, times(2)).createInstitution(createInstitutionCaptor.capture());
    List<LocationUnitClient.LocationUnit> institutions = createInstitutionCaptor.getAllValues();
    assertThat(institutions)
      .extracting(List::getFirst,l->l.get(1))
      .extracting("name", "code")
      .containsExactly(
        tuple("Agency Name 1", "agencyCode1"),
        tuple("Agency Name 2", "agencyCode2")
      );


    // Verify institution queries
    ArgumentCaptor<String> institutionsStringArgCaptor = ArgumentCaptor.forClass(String.class);
    verify(locationUnitClient, times(2)).findInstitutionsByQuery(institutionsStringArgCaptor.capture(), anyBoolean(), anyInt(), anyInt());
    assertThat(institutionsStringArgCaptor.getAllValues())
      .extracting(List::getFirst,l->l.get(1))
      .containsExactly(
        formatAgencyQuery("Agency Name 1", "agencyCode1"),
        formatAgencyQuery("Agency Name 2", "agencyCode2")
      );
  }

  private void validateCampus() {
    ArgumentCaptor<LocationUnitClient.LocationUnit> createCampusCaptor = ArgumentCaptor.forClass(LocationUnitClient.LocationUnit.class);
    // Verify campus creation
    verify(locationUnitClient, times(2)).createCampus(createCampusCaptor.capture());
    List<LocationUnitClient.LocationUnit> campuses = createCampusCaptor.getAllValues();
    assertThat(campuses)
      .extracting(List::getFirst,l->l.get(1))
      .extracting("name", "code")
      .containsExactly(
        tuple("Agency Name 1", "agencyCode1"),
        tuple("Agency Name 2", "agencyCode2")
      );

    // Verify campus queries
    ArgumentCaptor<String> campusesStringArgCaptor = ArgumentCaptor.forClass(String.class);
    verify(locationUnitClient, times(2)).findCampusesByQuery(campusesStringArgCaptor.capture(), anyBoolean(), anyInt(), anyInt());
    assertThat(campusesStringArgCaptor.getAllValues())
      .extracting(List::getFirst,l->l.get(1))
      .containsExactly(
        formatAgencyQuery("Agency Name 1", "agencyCode1"),
        formatAgencyQuery("Agency Name 2", "agencyCode2")
      );
  }

  private void validateLibrary() {
    ArgumentCaptor<LocationUnitClient.LocationUnit> createLibraryCaptor = ArgumentCaptor.forClass(LocationUnitClient.LocationUnit.class);

    // Verify library creation
    verify(locationUnitClient, times(2)).createLibrary(createLibraryCaptor.capture());
    List<LocationUnitClient.LocationUnit> libraries = createLibraryCaptor.getAllValues();
    assertThat(libraries)
      .extracting(List::getFirst,l->l.get(1))
      .extracting("name", "code")
      .containsExactly(
        tuple("Agency Name 1", "agencyCode1"),
        tuple("Agency Name 2", "agencyCode2")
      );

    // Verify library queries
    ArgumentCaptor<String> librariesStringArgCaptor = ArgumentCaptor.forClass(String.class);
    verify(locationUnitClient, times(2)).findCampusesByQuery(librariesStringArgCaptor.capture(), anyBoolean(), anyInt(), anyInt());
    assertThat(librariesStringArgCaptor.getAllValues())
      .extracting(List::getFirst,l->l.get(1))
      .containsExactly(
        formatAgencyQuery("Agency Name 1", "agencyCode1"),
        formatAgencyQuery("Agency Name 2", "agencyCode2")
      );
  }

  private void validateLocations() {
    ArgumentCaptor<LocationsClient.LocationDTO> createLocationCaptor = ArgumentCaptor.forClass(LocationsClient.LocationDTO.class);

    // Verify location creation
    verify(locationsClient, times(2)).createLocation(createLocationCaptor.capture());
    List<LocationsClient.LocationDTO> locations = createLocationCaptor.getAllValues();
    assertThat(locations)
      .extracting(List::getFirst,l->l.get(1))
      .extracting("name", "code")
      .containsExactly(
        tuple("Location 1", "loc1"),
        tuple("Location 2", "loc2")
      );

    // Verify library queries
    ArgumentCaptor<String> locationStringArgCaptor = ArgumentCaptor.forClass(String.class);
    verify(locationsClient, times(2)).findLocationByQuery(locationStringArgCaptor.capture(), eq(true), anyInt(), anyInt());
    assertThat(locationStringArgCaptor.getAllValues())
      .extracting(List::getFirst,l->l.get(1))
      .containsExactly(
        formatAgencyQuery("Location 1", "loc1"),
        formatAgencyQuery("Location 2", "loc2")
      );
  }

  @Test
  void createShadowLocations_EmptyResponse_NoLocationsCreated() {
    // Given
    DcbHubLocationResponse emptyResponse = new DcbHubLocationResponse();
    emptyResponse.setContent(Collections.emptyList());
    emptyResponse.setTotalSize(0);
    emptyResponse.setTotalPages(0);

    // Mocking
    when(dcbHubLocationClient.getLocations(anyInt(), anyInt(), anyString()))
      .thenReturn(emptyResponse);

    // When
    dcbHubLocationService.createShadowLocations(false);

    // Then
    verify(dcbHubLocationClient).getLocations(1, 5, BEARER_TOKEN);
    verify(locationUnitClient, never()).createInstitution(any());
    verify(locationUnitClient, never()).createCampus(any());
    verify(locationUnitClient, never()).createLibrary(any());
    verify(locationsClient, never()).createLocation(any());
  }

  @Test
  void createShadowLocations_ExistingLocationUnits_AgenciesNotCalled_Success() {
    // Given
    DcbLocation location = createTestLocation("Location 1", "loc1", "Agency Name 1", "agencyCode1");
    DcbHubLocationResponse response = new DcbHubLocationResponse();
    response.setContent(Collections.singletonList(location));
    response.setTotalPages(1);

    // Mocking
    when(dcbHubLocationClient.getLocations(1, 5, BEARER_TOKEN))
      .thenReturn(response);

    mockLocationUnitResponses("Agency Name 1", "agencyCode1");
    mockEmptyLocationDTOResponses("Location 1", "loc1");

    // When
    dcbHubLocationService.createShadowLocations(false);

    // Then
    verify(dcbHubLocationClient).getLocations(1, 5, BEARER_TOKEN);
    verify(locationUnitClient, never()).createInstitution(any());
    verify(locationUnitClient, never()).createCampus(any());
    verify(locationUnitClient, never()).createLibrary(any());
    verify(locationsClient).createLocation(any());
  }

  @Test
  void createShadowLocations_ExistingLocationDTO_createLocationNotCalled_Success() {
    // Given
    DcbLocation location = createTestLocation("Location 1", "loc1", "Agency Name 1", "agencyCode1");
    DcbHubLocationResponse response = new DcbHubLocationResponse();
    response.setContent(Collections.singletonList(location));
    response.setTotalPages(1);

    // Mocking
    when(dcbHubLocationClient.getLocations(1, 5, BEARER_TOKEN))
      .thenReturn(response);

    mockEmptyLocationUnitResponses("Agency Name 1", "agencyCode1");
    mockLocationDTOResponses("Location 1", "loc1");

    // When
    dcbHubLocationService.createShadowLocations(false);

    // Then
    verify(dcbHubLocationClient).getLocations(1, 5, BEARER_TOKEN);
    verify(locationUnitClient).createInstitution(any());
    verify(locationUnitClient).createCampus(any());
    verify(locationUnitClient).createLibrary(any());
    verify(locationsClient, never()).createLocation(any());
  }

  @Test
  void createShadowLocations_FeignException_ThrowsDcbHubLocationException() {
    // Given
    when(dcbHubLocationClient.getLocations(anyInt(), anyInt(), anyString()))
      .thenThrow(FeignException.class);

    // Then
    assertThrows(DcbHubLocationException.class, () ->
      dcbHubLocationService.createShadowLocations(false));
  }

  @Test
  void createShadowLocations_Exception_ThrowsServiceException() {
    // Given
    when(dcbHubLocationClient.getLocations(anyInt(), anyInt(), anyString()))
      .thenThrow(RuntimeException.class);

    // Then
    assertThrows(RuntimeException.class, () ->
      dcbHubLocationService.createShadowLocations(false));
  }

  private DcbLocation createTestLocation(String locationName, String locationCode, String agencyName, String agencyCode) {
    DcbLocation location = new DcbLocation();
    location.setCode(locationCode);
    location.setName(locationName);
    DcbAgency agency = new DcbAgency();
    agency.setCode(agencyCode);
    agency.setName(agencyName);
    location.setAgency(agency);
    return location;
  }

  private void mockLocationUnitResponses(String agencyName, String agencyCode) {
    ResultList<LocationUnitClient.LocationUnit> locationUnitResult = new ResultList<>();
    LocationUnitClient.LocationUnit locationUnit = LocationUnitClient.LocationUnit.builder()
      .id(UUID.randomUUID().toString()).code(agencyCode).name(agencyName).build();
    locationUnitResult.setResult(List.of(locationUnit));

    when(locationUnitClient.findInstitutionsByQuery(formatAgencyQuery(agencyName, agencyCode), true, 10, 0))
      .thenReturn(locationUnitResult);
    when(locationUnitClient.findCampusesByQuery(formatAgencyQuery(agencyName, agencyCode), true, 10, 0))
      .thenReturn(locationUnitResult);
    when(locationUnitClient.findLibrariesByQuery(formatAgencyQuery(agencyName, agencyCode), true, 10, 0))
      .thenReturn(locationUnitResult);
  }

  private void mockLocationDTOResponses(String locationName, String locationCode) {
    ResultList<LocationsClient.LocationDTO> mockLocationDTO = new ResultList<>();
    LocationsClient.LocationDTO locationDTO = LocationsClient.LocationDTO.builder()
      .id(UUID.randomUUID().toString()).name(locationName).code(locationCode).build();
    mockLocationDTO.setResult(List.of(locationDTO));

    when(locationsClient.findLocationByQuery(formatAgencyQuery(locationName, locationCode), true, 10, 0))
      .thenReturn(mockLocationDTO);
  }

  private void mockEmptyLocationUnitResponses(String agencyName, String agencyCode) {
    ResultList<LocationUnitClient.LocationUnit> emptyLocationUnit = new ResultList<>();
    emptyLocationUnit.setResult(Collections.emptyList());

    when(locationUnitClient.findInstitutionsByQuery(formatAgencyQuery(agencyName, agencyCode), true,  10, 0))
      .thenReturn(emptyLocationUnit);
    when(locationUnitClient.findCampusesByQuery(formatAgencyQuery(agencyName, agencyCode), true,  10, 0))
      .thenReturn(emptyLocationUnit);
    when(locationUnitClient.findLibrariesByQuery(formatAgencyQuery(agencyName, agencyCode), true,  10, 0))
      .thenReturn(emptyLocationUnit);
  }

  private void mockEmptyLocationDTOResponses(String locationName, String locationCode) {
    ResultList<LocationsClient.LocationDTO> emptyLocationDTO = new ResultList<>();
    emptyLocationDTO.setResult(Collections.emptyList());

    when(locationsClient.findLocationByQuery(formatAgencyQuery(locationName, locationCode), true, 10, 0))
      .thenReturn(emptyLocationDTO);

  }

  private String formatAgencyQuery(String name, String code) {
    return CqlQuery.byNameAndCode(name, code);
  }
}
