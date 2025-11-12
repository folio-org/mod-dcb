package org.folio.dcb.service.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.utils.CqlQuery.exactMatchById;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.folio.dcb.client.feign.InstanceClient;
import org.folio.dcb.client.feign.InstanceClient.InventoryInstanceDTO;
import org.folio.dcb.client.feign.InstanceTypeClient.InstanceType;
import org.folio.dcb.domain.ResultList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DcbInstanceServiceTest {

  private static final String TEST_SOURCE = "FOLIO";
  private static final String TEST_INSTANCE_TITLE = "DCB_INSTANCE";
  private static final String TEST_INSTANCE_ID = "9d1b77e4-f02e-4b7f-b296-3f2042ddac54";
  private static final String TEST_INSTANCE_TYPE_ID = "9d1b77e0-f02e-4b7f-b296-3f2042ddac54";

  @InjectMocks private DcbInstanceService dcbInstanceService;
  @Mock private InstanceClient instanceClient;
  @Mock private DcbInstanceTypeService dcbInstanceTypeService;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(instanceClient, dcbInstanceTypeService);
  }

  @Test
  void findDcbEntity_positive_shouldReturnInstanceWhenExists() {
    var foundInstances = ResultList.asSinglePage(dcbInstance());
    var expectedQuery = exactMatchById(TEST_INSTANCE_ID);
    when(instanceClient.findByQuery(expectedQuery)).thenReturn(foundInstances);

    var result = dcbInstanceService.findDcbEntity();

    assertThat(result).contains(dcbInstance());
  }

  @Test
  void findDcbEntity_positive_shouldReturnEmptyWhenNotExists() {
    var expectedQuery = exactMatchById(TEST_INSTANCE_ID);
    when(instanceClient.findByQuery(expectedQuery)).thenReturn(ResultList.empty());

    var result = dcbInstanceService.findDcbEntity();

    assertThat(result).isEmpty();
  }

  @Test
  void createDcbEntity_positive_shouldCreateInstanceWithInstanceType() {
    var instanceType = dcbInstanceType();

    when(dcbInstanceTypeService.findOrCreateEntity()).thenReturn(instanceType);

    var result = dcbInstanceService.createDcbEntity();

    assertThat(result).isEqualTo(dcbInstance());
    verify(dcbInstanceTypeService).findOrCreateEntity();
    verify(instanceClient).createInstance(any(InventoryInstanceDTO.class));
  }

  @Test
  void getDefaultValue_positive_shouldReturnInstanceWithDefaultValues() {
    var result = dcbInstanceService.getDefaultValue();

    assertThat(result).isEqualTo(dcbInstance());
  }

  @Test
  void findOrCreateEntity_positive_shouldReturnExistingInstance() {
    var expectedQuery = exactMatchById(TEST_INSTANCE_ID);
    var instancesResult = ResultList.asSinglePage(dcbInstance());
    when(instanceClient.findByQuery(expectedQuery)).thenReturn(instancesResult);

    var result = dcbInstanceService.findOrCreateEntity();

    assertThat(result).isEqualTo(dcbInstance());
    verify(dcbInstanceTypeService, never()).findOrCreateEntity();
    verify(instanceClient, never()).createInstance(any());
  }

  private static InventoryInstanceDTO dcbInstance() {
    return InventoryInstanceDTO.builder()
      .id(TEST_INSTANCE_ID)
      .instanceTypeId(TEST_INSTANCE_TYPE_ID)
      .title(TEST_INSTANCE_TITLE)
      .source(TEST_SOURCE)
      .build();
  }

  private static InstanceType dcbInstanceType() {
    return InstanceType.builder()
      .id(TEST_INSTANCE_TYPE_ID)
      .build();
  }
}
