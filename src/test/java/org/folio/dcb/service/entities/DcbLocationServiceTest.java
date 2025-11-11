package org.folio.dcb.service.entities;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.utils.CqlQuery.exactMatchByName;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.folio.dcb.client.feign.LocationUnitClient.LocationUnit;
import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.client.feign.LocationsClient.LocationDTO;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DcbLocationServiceTest {

  private static final String TEST_LOCATION_ID = "9d1b77e8-f02e-4b7f-b296-3f2042ddac54";
  private static final String TEST_LIBRARY_ID = "9d1b77e7-f02e-4b7f-b296-3f2042ddac54";
  private static final String TEST_CAMPUS_ID = "9d1b77e6-f02e-4b7f-b296-3f2042ddac54";
  private static final String TEST_INSTITUTION_ID = "9d1b77e5-f02e-4b7f-b296-3f2042ddac54";
  private static final String TEST_SERVICE_POINT_ID = "9d1b77e8-f02e-4b7f-b296-3f2042ddac54";
  private static final String TEST_NAME = "DCB";
  private static final String TEST_CODE = "000";

  @InjectMocks private DcbLocationService dcbLocationService;
  @Mock private LocationsClient locationsClient;
  @Mock private DcbLibraryService dcbLibraryService;
  @Mock private DcbCampusService dcbCampusService;
  @Mock private DcbInstitutionService dcbInstitutionService;
  @Mock private DcbServicePointService dcbServicePointService;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(locationsClient, dcbLibraryService,
      dcbCampusService, dcbInstitutionService, dcbServicePointService);
  }

  @Test
  void findDcbEntity_positive_shouldReturnLocationWhenExists() {
    var foundLocations = ResultList.asSinglePage(dcbLocation());
    var expectedQuery = exactMatchByName(TEST_NAME);
    when(locationsClient.findByQuery(expectedQuery)).thenReturn(foundLocations);

    var result = dcbLocationService.findDcbEntity();

    assertThat(result).contains(dcbLocation());
  }

  @Test
  void findDcbEntity_positive_shouldReturnEmptyWhenNotExists() {
    var expectedQuery = exactMatchByName(TEST_NAME);
    when(locationsClient.findByQuery(expectedQuery)).thenReturn(ResultList.empty());

    var result = dcbLocationService.findDcbEntity();

    assertThat(result).isEmpty();
  }

  @Test
  void createDcbEntity_positive_shouldCreateLocationWithLibraryAndServicePoint() {
    var expectedLocation = dcbLocation();

    when(dcbLibraryService.findOrCreateEntity()).thenReturn(dcbLibrary());
    when(dcbCampusService.findOrCreateEntity()).thenReturn(dcbCampus());
    when(dcbInstitutionService.findOrCreateEntity()).thenReturn(dcbInstitution());
    when(dcbServicePointService.findOrCreateEntity()).thenReturn(dcbServicePoint());
    when(locationsClient.createLocation(expectedLocation)).thenReturn(expectedLocation);

    var result = dcbLocationService.createDcbEntity();

    assertThat(result).isEqualTo(dcbLocation());
    verify(dcbLibraryService).findOrCreateEntity();
    verify(dcbServicePointService).findOrCreateEntity();
    verify(locationsClient).createLocation(expectedLocation);
  }

  @Test
  void getDefaultValue_positive_shouldReturnLocationWithDefaultValues() {
    var result = dcbLocationService.getDefaultValue();
    assertThat(result).isEqualTo(dcbLocation());
  }

  @Test
  void findOrCreateEntity_positive_shouldReturnExistingLocation() {
    var expectedQuery = exactMatchByName(TEST_NAME);
    var locationsResult = ResultList.asSinglePage(dcbLocation());
    when(locationsClient.findByQuery(expectedQuery)).thenReturn(locationsResult);

    var result = dcbLocationService.findOrCreateEntity();

    assertThat(result).isEqualTo(dcbLocation());
    verify(dcbLibraryService, never()).findOrCreateEntity();
    verify(dcbServicePointService, never()).findOrCreateEntity();
    verify(locationsClient, never()).createLocation(any());
  }

  private static LocationDTO dcbLocation() {
    return LocationDTO.builder()
      .id(TEST_LOCATION_ID)
      .institutionId(TEST_INSTITUTION_ID)
      .campusId(TEST_CAMPUS_ID)
      .libraryId(TEST_LIBRARY_ID)
      .primaryServicePoint(TEST_SERVICE_POINT_ID)
      .code(TEST_CODE)
      .name(TEST_NAME)
      .servicePointIds(singletonList(TEST_SERVICE_POINT_ID))
      .build();
  }

  private static LocationUnit dcbLibrary() {
    return LocationUnit.builder()
      .id(TEST_LIBRARY_ID)
      .name(TEST_NAME)
      .code(TEST_CODE)
      .campusId(TEST_CAMPUS_ID)
      .build();
  }

  private LocationUnit dcbCampus() {
    return LocationUnit.builder()
      .id(TEST_CAMPUS_ID)
      .name(TEST_NAME)
      .code(TEST_CODE)
      .institutionId(TEST_INSTITUTION_ID)
      .build();
  }

  private LocationUnit dcbInstitution() {
    return LocationUnit.builder()
      .id(TEST_INSTITUTION_ID)
      .name(TEST_NAME)
      .code(TEST_CODE)
      .build();
  }

  private static ServicePointRequest dcbServicePoint() {
    return ServicePointRequest.builder()
      .id(TEST_SERVICE_POINT_ID)
      .build();
  }
}
