package org.folio.dcb.service.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.utils.CqlQuery.exactMatchByName;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.folio.dcb.client.feign.LocationUnitClient;
import org.folio.dcb.client.feign.LocationUnitClient.LocationUnit;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DcbLibraryServiceTest {

  private static final String TEST_LIBRARY_ID = "9d1b77e7-f02e-4b7f-b296-3f2042ddac54";
  private static final String TEST_CAMPUS_ID = "9d1b77e6-f02e-4b7f-b296-3f2042ddac54";
  private static final String TEST_NAME = "DCB";
  private static final String TEST_CODE = "000";

  @InjectMocks private DcbLibraryService dcbLibraryService;
  @Mock private DcbCampusService dcbCampusService;
  @Mock private LocationUnitClient locationUnitClient;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(dcbCampusService, locationUnitClient);
  }

  @Test
  void findDcbEntity_positive_shouldReturnLibraryWhenExists() {
    var foundLibraries = ResultList.asSinglePage(dcbLibrary());
    var expectedQuery = exactMatchByName(TEST_NAME);
    when(locationUnitClient.findLibrariesByQuery(expectedQuery)).thenReturn(foundLibraries);

    var result = dcbLibraryService.findDcbEntity();

    assertThat(result).contains(dcbLibrary());
  }

  @Test
  void findDcbEntity_positive_shouldReturnEmptyWhenNotExists() {
    var expectedQuery = exactMatchByName(TEST_NAME);
    when(locationUnitClient.findLibrariesByQuery(expectedQuery)).thenReturn(ResultList.empty());
    var result = dcbLibraryService.findDcbEntity();
    assertThat(result).isEmpty();
  }

  @Test
  void createDcbEntity_positive_shouldCreateLibraryWithCampus() {
    var campus = dcbCampus();
    var expectedLibrary = dcbLibrary();

    when(dcbCampusService.findOrCreateEntity()).thenReturn(campus);
    when(locationUnitClient.createLibrary(expectedLibrary)).thenReturn(expectedLibrary);

    var result = dcbLibraryService.createDcbEntity();

    assertThat(result).isEqualTo(dcbLibrary());
    verify(locationUnitClient).createLibrary(expectedLibrary);
  }

  @Test
  void getDefaultValue_positive_shouldReturnLibraryWithDefaultValues() {
    var result = dcbLibraryService.getDefaultValue();
    assertThat(result).isEqualTo(dcbLibrary());
  }

  @Test
  void findOrCreateEntity_positive_shouldReturnExistingLibrary() {
    var expectedQuery = exactMatchByName(TEST_NAME);
    var librariesResult = ResultList.asSinglePage(dcbLibrary());
    when(locationUnitClient.findLibrariesByQuery(expectedQuery)).thenReturn(librariesResult);

    var result = dcbLibraryService.findOrCreateEntity();

    assertThat(result).isEqualTo(dcbLibrary());
    verify(dcbCampusService, never()).findOrCreateEntity();
    verify(locationUnitClient, never()).createCampus(any());
  }

  private static LocationUnit dcbLibrary() {
    return LocationUnit.builder()
      .id(TEST_LIBRARY_ID)
      .campusId(TEST_CAMPUS_ID)
      .name(TEST_NAME)
      .code(TEST_CODE)
      .build();
  }

  private static LocationUnit dcbCampus() {
    return LocationUnit.builder()
      .id(TEST_CAMPUS_ID)
      .build();
  }
}
