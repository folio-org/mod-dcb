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
class DcbInstitutionServiceTest {

  private static final String TEST_INSTITUTION_ID = "9d1b77e5-f02e-4b7f-b296-3f2042ddac54";
  private static final String TEST_NAME = "DCB";
  private static final String TEST_CODE = "000";

  @InjectMocks private DcbInstitutionService dcbInstitutionService;
  @Mock private LocationUnitClient locationUnitClient;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(locationUnitClient);
  }

  @Test
  void findDcbEntity_positive_shouldReturnInstitutionWhenExists() {
    var foundInstitutions = ResultList.asSinglePage(dcbInstitution());
    var expectedQuery = exactMatchByName(TEST_NAME);
    when(locationUnitClient.findInstitutionsByQuery(expectedQuery)).thenReturn(foundInstitutions);

    var result = dcbInstitutionService.findDcbEntity();

    assertThat(result).contains(dcbInstitution());
  }

  @Test
  void findDcbEntity_positive_shouldReturnEmptyWhenNotExists() {
    var expectedQuery = exactMatchByName(TEST_NAME);
    when(locationUnitClient.findInstitutionsByQuery(expectedQuery)).thenReturn(ResultList.empty());
    var result = dcbInstitutionService.findDcbEntity();
    assertThat(result).isEmpty();
  }

  @Test
  void createDcbEntity_positive_shouldCreateInstitution() {
    var expectedInstitution = dcbInstitution();
    when(locationUnitClient.createInstitution(expectedInstitution)).thenReturn(expectedInstitution);

    var result = dcbInstitutionService.createDcbEntity();

    assertThat(result).isEqualTo(dcbInstitution());
    verify(locationUnitClient).createInstitution(expectedInstitution);
  }

  @Test
  void getDefaultValue_positive_shouldReturnInstitutionWithDefaultValues() {
    var result = dcbInstitutionService.getDefaultValue();
    assertThat(result).isEqualTo(dcbInstitution());
  }

  @Test
  void findOrCreateEntity_positive_shouldReturnExistingInstitution() {
    var expectedQuery = exactMatchByName(TEST_NAME);
    var institutionsResult = ResultList.asSinglePage(dcbInstitution());
    when(locationUnitClient.findInstitutionsByQuery(expectedQuery)).thenReturn(institutionsResult);

    var result = dcbInstitutionService.findOrCreateEntity();

    assertThat(result).isEqualTo(dcbInstitution());
    verify(locationUnitClient, never()).createInstitution(any());
  }

  private static LocationUnit dcbInstitution() {
    return LocationUnit.builder()
      .id(TEST_INSTITUTION_ID)
      .name(TEST_NAME)
      .code(TEST_CODE)
      .build();
  }
}
