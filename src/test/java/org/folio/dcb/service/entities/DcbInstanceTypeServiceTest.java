package org.folio.dcb.service.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.utils.CqlQuery.exactMatchByName;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.folio.dcb.client.feign.InstanceTypeClient;
import org.folio.dcb.client.feign.InstanceTypeClient.InstanceType;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DcbInstanceTypeServiceTest {

  private static final String TEST_CODE = "000";
  private static final String TEST_NAME = "DCB";
  private static final String TEST_SOURCE = "local";
  private static final String TEST_INSTANCE_TYPE_ID = "9d1b77e0-f02e-4b7f-b296-3f2042ddac54";

  @InjectMocks private DcbInstanceTypeService dcbInstanceTypeService;
  @Mock private InstanceTypeClient instanceTypeClient;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(instanceTypeClient);
  }

  @Test
  void findDcbEntity_positive_shouldReturnInstanceTypeWhenExists() {
    var foundInstanceTypes = ResultList.asSinglePage(dcbInstanceType());
    var expectedQuery = exactMatchByName(TEST_NAME);
    when(instanceTypeClient.findByQuery(expectedQuery)).thenReturn(foundInstanceTypes);

    var result = dcbInstanceTypeService.findDcbEntity();

    assertThat(result).contains(dcbInstanceType());
  }

  @Test
  void findDcbEntity_positive_shouldReturnEmptyWhenNotExists() {
    var expectedQuery = exactMatchByName(TEST_NAME);
    when(instanceTypeClient.findByQuery(expectedQuery)).thenReturn(ResultList.empty());
    var result = dcbInstanceTypeService.findDcbEntity();
    assertThat(result).isEmpty();
  }

  @Test
  void createDcbEntity_positive_shouldCreateInstanceType() {
    var expectedInstanceType = dcbInstanceType();

    var result = dcbInstanceTypeService.createDcbEntity();

    assertThat(result).isEqualTo(dcbInstanceType());
    verify(instanceTypeClient).createInstanceType(expectedInstanceType);
  }

  @Test
  void getDefaultValue_positive_shouldReturnInstanceTypeWithDefaultValues() {
    var result = dcbInstanceTypeService.getDefaultValue();
    assertThat(result).isEqualTo(dcbInstanceType());
  }

  @Test
  void findOrCreateEntity_positive_shouldReturnExistingInstanceType() {
    var expectedQuery = exactMatchByName(TEST_NAME);
    var instanceTypesResult = ResultList.asSinglePage(dcbInstanceType());
    when(instanceTypeClient.findByQuery(expectedQuery)).thenReturn(instanceTypesResult);

    var result = dcbInstanceTypeService.findOrCreateEntity();

    assertThat(result).isEqualTo(dcbInstanceType());
    verify(instanceTypeClient, never()).createInstanceType(any());
  }

  private static InstanceType dcbInstanceType() {
    return InstanceType.builder()
      .id(TEST_INSTANCE_TYPE_ID)
      .code(TEST_CODE)
      .name(TEST_NAME)
      .source(TEST_SOURCE)
      .build();
  }
}
