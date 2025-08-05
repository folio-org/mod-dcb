package org.folio.dcb.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import org.folio.dcb.service.impl.DcbHubLocationServiceImpl;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import feign.FeignException;

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

  @InjectMocks
  private DcbHubLocationServiceImpl dcbHubLocationService;

  private ServicePointRequest servicePointRequest;
  private static final String BEARER_TOKEN = "test-token";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(dcbHubLocationService, "batchSize", 5);
    DcbHubKCCredentials credentials = new DcbHubKCCredentials();
    credentials.setClientId("test-client");
    credentials.setClientSecret("test-secret");

    servicePointRequest = new ServicePointRequest();
    servicePointRequest.setId("sp-123");

    when(dcbHubKCCredentialSecureStore.getDcbHubKCCredentials()).thenReturn(credentials);
    when(dcbHubKCTokenService.getBearerAccessToken(any())).thenReturn(BEARER_TOKEN);
  }

  @Test
  void createShadowLocations_SinglePage_AgencyAndLocationNotExistAlready_Success() {
    // Given
    LocationResponse.Location location1 = createTestLocation("Location 1", "loc1", "Agency Name 1", "agencyCode1");
    LocationResponse.Location location2 = createTestLocation("Location 2", "loc2", "Agency Name 1", "agencyCode1");

    LocationResponse response = new LocationResponse();
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
    dcbHubLocationService.createShadowLocations(locationsClient, locationUnitClient, servicePointRequest);

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
    LocationResponse.Location location1 = createTestLocation( "Location 1", "loc1",  "Agency Name 1", "agencyCode1");
    LocationResponse.Location location2 = createTestLocation("Location 2", "loc2",  "Agency Name 1", "agencyCode1");

    LocationResponse response = new LocationResponse();
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
    dcbHubLocationService.createShadowLocations(locationsClient, locationUnitClient, servicePointRequest);

    // Then
    verify(dcbHubLocationClient).getLocations(1, 5, BEARER_TOKEN);
    verify(locationUnitClient, never()).createInstitution(any());
    verify(locationUnitClient).queryInstitutionByNameAndCode(anyString(), anyString());
    verify(locationUnitClient, never()).createCampus(any());
    verify(locationUnitClient).queryCampusByNameAndCode(anyString(), anyString());
    verify(locationUnitClient, never()).createLibrary(any());
    verify(locationUnitClient).queryLibraryByNameAndCode(anyString(), anyString());
    verify(locationsClient, never()).createLocation(any());
    verify(locationsClient, times(2)).queryLocationsByNameAndCode(anyString(), anyString());
  }

  @Test
  void createShadowLocations_MultiplePages_Success() {
    // Given
    LocationResponse.Location location1 = createTestLocation( "Location 1", "loc1", "Agency Name 1", "agencyCode1");
    LocationResponse.Location location2 = createTestLocation("Location 2", "loc2", "Agency Name 2", "agencyCode2");

    LocationResponse page1 = new LocationResponse();
    page1.setContent(Collections.singletonList(location1));
    page1.setTotalSize(2);
    page1.setTotalPages(2);

    LocationResponse page2 = new LocationResponse();
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
    dcbHubLocationService.createShadowLocations(locationsClient, locationUnitClient, servicePointRequest);

    // Then
    // ArgumentCaptors for LocationUnit operations
    ArgumentCaptor<LocationUnitClient.LocationUnit> createInstitutionCaptor = ArgumentCaptor.forClass(LocationUnitClient.LocationUnit.class);
    ArgumentCaptor<LocationUnitClient.LocationUnit> createCampusCaptor = ArgumentCaptor.forClass(LocationUnitClient.LocationUnit.class);
    ArgumentCaptor<LocationUnitClient.LocationUnit> createLibraryCaptor = ArgumentCaptor.forClass(LocationUnitClient.LocationUnit.class);

    // ArgumentCaptors for query operations
    ArgumentCaptor<String> queryInstitutionNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> queryInstitutionCodeCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> queryCampusNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> queryCampusCodeCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> queryLibraryNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> queryLibraryCodeCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> queryLocationNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> queryLocationCodeCaptor = ArgumentCaptor.forClass(String.class);

    // ArgumentCaptor for LocationDTO operations
    ArgumentCaptor<LocationsClient.LocationDTO> createLocationCaptor = ArgumentCaptor.forClass(LocationsClient.LocationDTO.class);

    verify(dcbHubLocationClient).getLocations(1, 5, BEARER_TOKEN);
    verify(dcbHubLocationClient).getLocations(2, 5, BEARER_TOKEN);

    validateInstitutions(createInstitutionCaptor, queryInstitutionNameCaptor, queryInstitutionCodeCaptor);

    validateCampus(createCampusCaptor, queryCampusNameCaptor, queryCampusCodeCaptor);

    validateLibrary(createLibraryCaptor, queryLibraryNameCaptor, queryLibraryCodeCaptor);

    validateLocations(createLocationCaptor, queryLocationNameCaptor, queryLocationCodeCaptor);
  }

  private void validateInstitutions(ArgumentCaptor<LocationUnitClient.LocationUnit> createInstitutionCaptor,
    ArgumentCaptor<String> queryInstitutionNameCaptor, ArgumentCaptor<String> queryInstitutionCodeCaptor) {
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
    verify(locationUnitClient, times(2)).queryInstitutionByNameAndCode(
      queryInstitutionNameCaptor.capture(),
      queryInstitutionCodeCaptor.capture()
    );
    List<String> institutionNames = queryInstitutionNameCaptor.getAllValues();
    List<String> institutionCodes = queryInstitutionCodeCaptor.getAllValues();
    assertThat(institutionNames)
      .extracting(List::getFirst,l->l.get(1))
      .containsExactly("Agency Name 1", "Agency Name 2");
    assertThat(institutionCodes)
      .extracting(List::getFirst,l->l.get(1))
      .containsExactly("agencyCode1", "agencyCode2");
  }

  private void validateCampus(ArgumentCaptor<LocationUnitClient.LocationUnit> createCampusCaptor,
    ArgumentCaptor<String> queryCampusNameCaptor, ArgumentCaptor<String> queryCampusCodeCaptor) {
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
    verify(locationUnitClient, times(2)).queryCampusByNameAndCode(
      queryCampusNameCaptor.capture(),
      queryCampusCodeCaptor.capture()
    );
    List<String> campusNames = queryCampusNameCaptor.getAllValues();
    List<String> campusCodes = queryCampusCodeCaptor.getAllValues();
    assertThat(campusNames)
      .extracting(List::getFirst,l->l.get(1))
      .containsExactly("Agency Name 1", "Agency Name 2");
    assertThat(campusCodes)
      .extracting(List::getFirst,l->l.get(1))
      .containsExactly("agencyCode1", "agencyCode2");
  }

  private void validateLibrary(ArgumentCaptor<LocationUnitClient.LocationUnit> createLibraryCaptor,
    ArgumentCaptor<String> queryLibraryNameCaptor, ArgumentCaptor<String> queryLibraryCodeCaptor) {
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
    verify(locationUnitClient, times(2)).queryLibraryByNameAndCode(
      queryLibraryNameCaptor.capture(),
      queryLibraryCodeCaptor.capture()
    );
    List<String> libraryNames = queryLibraryNameCaptor.getAllValues();
    List<String> libraryCodes = queryLibraryCodeCaptor.getAllValues();
    assertThat(libraryNames)
      .extracting(List::getFirst,l->l.get(1))
      .containsExactly("Agency Name 1", "Agency Name 2");
    assertThat(libraryCodes)
      .extracting(List::getFirst,l->l.get(1))
      .containsExactly("agencyCode1", "agencyCode2");
  }

  private void validateLocations(ArgumentCaptor<LocationsClient.LocationDTO> createLocationCaptor,
    ArgumentCaptor<String> queryLocationNameCaptor, ArgumentCaptor<String> queryLocationCodeCaptor) {
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

    // Verify location queries
    verify(locationsClient, times(2)).queryLocationsByNameAndCode(
      queryLocationNameCaptor.capture(),
      queryLocationCodeCaptor.capture()
    );
    List<String> locationNames = queryLocationNameCaptor.getAllValues();
    List<String> locationCodes = queryLocationCodeCaptor.getAllValues();
    assertThat(locationNames)
      .extracting(List::getFirst,l->l.get(1))
      .containsExactly("Location 1", "Location 2");
    assertThat(locationCodes)
      .extracting(List::getFirst,l->l.get(1))
      .containsExactly("loc1", "loc2");
  }

  @Test
  void createShadowLocations_EmptyResponse_NoLocationsCreated() {
    // Given
    LocationResponse emptyResponse = new LocationResponse();
    emptyResponse.setContent(Collections.emptyList());
    emptyResponse.setTotalSize(0);
    emptyResponse.setTotalPages(0);

    // Mocking
    when(dcbHubLocationClient.getLocations(anyInt(), anyInt(), anyString()))
      .thenReturn(emptyResponse);

    // When
    dcbHubLocationService.createShadowLocations(locationsClient, locationUnitClient, servicePointRequest);

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
    LocationResponse.Location location = createTestLocation("Location 1", "loc1", "Agency Name 1", "agencyCode1");
    LocationResponse response = new LocationResponse();
    response.setContent(Collections.singletonList(location));
    response.setTotalPages(1);

    // Mocking
    when(dcbHubLocationClient.getLocations(1, 5, BEARER_TOKEN))
      .thenReturn(response);

    mockLocationUnitResponses("Agency Name 1", "agencyCode1");
    mockEmptyLocationDTOResponses("Location 1", "loc1");

    // When
    dcbHubLocationService.createShadowLocations(locationsClient, locationUnitClient, servicePointRequest);

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
    LocationResponse.Location location = createTestLocation("Location 1", "loc1", "Agency Name 1", "agencyCode1");
    LocationResponse response = new LocationResponse();
    response.setContent(Collections.singletonList(location));
    response.setTotalPages(1);

    // Mocking
    when(dcbHubLocationClient.getLocations(1, 5, BEARER_TOKEN))
      .thenReturn(response);

    mockEmptyLocationUnitResponses("Agency Name 1", "agencyCode1");
    mockLocationDTOResponses("Location 1", "loc1");

    // When
    dcbHubLocationService.createShadowLocations(locationsClient, locationUnitClient, servicePointRequest);

    // Then
    verify(dcbHubLocationClient).getLocations(1, 5, BEARER_TOKEN);
    verify(locationUnitClient).createInstitution(any());
    verify(locationUnitClient).createCampus(any());
    verify(locationUnitClient).createLibrary(any());
    verify(locationsClient, never()).createLocation(any());
  }

  @Test
  void createShadowLocations_FeignException_ThrowsServiceException() {
    // Given
    when(dcbHubLocationClient.getLocations(anyInt(), anyInt(), anyString()))
      .thenThrow(FeignException.class);

    // Then
    org.junit.jupiter.api.Assertions.assertThrows(ServiceException.class, () ->
      dcbHubLocationService.createShadowLocations(locationsClient, locationUnitClient, servicePointRequest));
  }

  @Test
  void createShadowLocations_Exception_ThrowsServiceException() {
    // Given
    when(dcbHubLocationClient.getLocations(anyInt(), anyInt(), anyString()))
      .thenThrow(RuntimeException.class);

    // Then
    org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
      dcbHubLocationService.createShadowLocations(locationsClient, locationUnitClient, servicePointRequest));
  }

  private LocationResponse.Location createTestLocation(String locationName, String locationCode, String agencyName, String agencyCode) {
    LocationResponse.Location location = new LocationResponse.Location();
    location.setCode(locationCode);
    location.setName(locationName);
    LocationResponse.Agency agency = new LocationResponse.Agency();
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

    when(locationUnitClient.queryInstitutionByNameAndCode(agencyName, agencyCode))
      .thenReturn(locationUnitResult);
    when(locationUnitClient.queryCampusByNameAndCode(agencyName, agencyCode))
      .thenReturn(locationUnitResult);
    when(locationUnitClient.queryLibraryByNameAndCode(agencyName, agencyCode))
      .thenReturn(locationUnitResult);
  }

  private void mockLocationDTOResponses(String locationName, String locationCode) {
    ResultList<LocationsClient.LocationDTO> emptyLocationDTO = new ResultList<>();
    LocationsClient.LocationDTO locationDTO = LocationsClient.LocationDTO.builder()
      .id(UUID.randomUUID().toString()).name(locationName).code(locationCode).build();
    emptyLocationDTO.setResult(List.of(locationDTO));

    when(locationsClient.queryLocationsByNameAndCode(locationName, locationCode))
      .thenReturn(emptyLocationDTO);
  }

  private void mockEmptyLocationUnitResponses(String agencyName, String agencyCode) {
    ResultList<LocationUnitClient.LocationUnit> emptyLocationUnit = new ResultList<>();
    emptyLocationUnit.setResult(Collections.emptyList());

    when(locationUnitClient.queryInstitutionByNameAndCode(agencyName, agencyCode))
      .thenReturn(emptyLocationUnit);
    when(locationUnitClient.queryCampusByNameAndCode(agencyName, agencyCode))
      .thenReturn(emptyLocationUnit);
    when(locationUnitClient.queryLibraryByNameAndCode(agencyName, agencyCode))
      .thenReturn(emptyLocationUnit);
  }

  private void mockEmptyLocationDTOResponses(String locationName, String locationCode) {
    ResultList<LocationsClient.LocationDTO> emptyLocationDTO = new ResultList<>();
    emptyLocationDTO.setResult(Collections.emptyList());

    when(locationsClient.queryLocationsByNameAndCode(locationName, locationCode))
      .thenReturn(emptyLocationDTO);
  }
}