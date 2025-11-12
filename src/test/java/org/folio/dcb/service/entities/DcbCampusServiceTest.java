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
import org.folio.dcb.domain.ResultList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DcbCampusServiceTest {

  private static final String TEST_CAMPUS_NAME = "DCB";
  private static final String TEST_CAMPUS_ID = "9d1b77e6-f02e-4b7f-b296-3f2042ddac54";
  private static final String TEST_INSTITUTION_ID = "9d1b77e5-f02e-4b7f-b296-3f2042ddac54";

  @InjectMocks private DcbCampusService dcbCampusService;
  @Mock private LocationUnitClient locationUnitClient;
  @Mock private DcbInstitutionService dcbInstitutionService;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(locationUnitClient, dcbInstitutionService);
  }

  @Test
  void findDcbEntity_positive_shouldReturnCampusWhenExists() {
    var foundCampuses = ResultList.asSinglePage(dcbCampus());
    var expectedQuery = exactMatchByName(TEST_CAMPUS_NAME);
    when(locationUnitClient.findCampusesByQuery(expectedQuery)).thenReturn(foundCampuses);

    var result = dcbCampusService.findDcbEntity();

    assertThat(result).contains(dcbCampus());
  }

  @Test
  void findDcbEntity_positive_shouldReturnEmptyWhenNotExists() {
    var expectedQuery = exactMatchByName(TEST_CAMPUS_NAME);
    when(locationUnitClient.findCampusesByQuery(expectedQuery)).thenReturn(ResultList.empty());
    var result = dcbCampusService.findDcbEntity();
    assertThat(result).isEmpty();
  }

  @Test
  void createDcbEntity_positive_shouldCreateCampusWithInstitution() {
    var institution = dcbInstitution();
    var expectedCampus = dcbCampus();

    when(dcbInstitutionService.findOrCreateEntity()).thenReturn(institution);
    when(locationUnitClient.createCampus(expectedCampus)).thenReturn(expectedCampus);

    var result = dcbCampusService.createDcbEntity();

    assertThat(result).isEqualTo(dcbCampus());
  }

  @Test
  void getDefaultValue_positive_shouldReturnCampusWithDefaultValues() {
    var result = dcbCampusService.getDefaultValue();
    assertThat(result).isEqualTo(dcbCampus());
  }

  @Test
  void findOrCreateEntity_positive_shouldReturnExistingCampus() {
    var expectedQuery = exactMatchByName(TEST_CAMPUS_NAME);
    var campusesResult = ResultList.asSinglePage(dcbCampus());
    when(locationUnitClient.findCampusesByQuery(expectedQuery)).thenReturn(campusesResult);

    var result = dcbCampusService.findOrCreateEntity();

    assertThat(result).isEqualTo(dcbCampus());
    verify(dcbInstitutionService, never()).findOrCreateEntity();
    verify(locationUnitClient, never()).createCampus(any());
  }

  private static LocationUnit dcbCampus() {
    return LocationUnit.builder()
      .id(TEST_CAMPUS_ID)
      .institutionId(TEST_INSTITUTION_ID)
      .name(TEST_CAMPUS_NAME)
      .code("000")
      .build();
  }

  private static LocationUnit dcbInstitution() {
    return LocationUnit.builder()
      .id(TEST_INSTITUTION_ID)
      .build();
  }
}
