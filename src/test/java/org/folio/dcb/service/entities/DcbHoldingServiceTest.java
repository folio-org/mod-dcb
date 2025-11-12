package org.folio.dcb.service.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.utils.CqlQuery.exactMatchById;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.dcb.client.feign.HoldingSourcesClient.HoldingSource;
import org.folio.dcb.client.feign.HoldingsStorageClient;
import org.folio.dcb.client.feign.HoldingsStorageClient.Holding;
import org.folio.dcb.client.feign.InstanceClient.InventoryInstanceDTO;
import org.folio.dcb.client.feign.LocationsClient.LocationDTO;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DcbHoldingServiceTest {

  private static final String TEST_HOLDING_ID = "10cd3a5a-d36f-4c7a-bc4f-e1ae3cf820c9";
  private static final String TEST_INSTANCE_ID = "9d1b77e4-f02e-4b7f-b296-3f2042ddac54";
  private static final String TEST_LOCATION_ID = "9d1b77e8-f02e-4b7f-b296-3f2042ddac54";
  private static final String TEST_HOLDING_SOURCE_ID = UUID.randomUUID().toString();

  @InjectMocks private DcbHoldingService dcbHoldingService;
  @Mock private DcbInstanceService dcbInstanceService;
  @Mock private DcbLocationService dcbLocationService;
  @Mock private HoldingsStorageClient holdingsStorageClient;
  @Mock private DcbHoldingSourceService dcbHoldingSourceService;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(dcbInstanceService, dcbLocationService,
      holdingsStorageClient, dcbHoldingSourceService);
  }

  @Test
  void findDcbEntity_positive_shouldReturnHoldingWhenExists() {
    var foundHoldings = ResultList.asSinglePage(dcbHolding());
    var expectedQuery = exactMatchById(TEST_HOLDING_ID);
    when(holdingsStorageClient.findByQuery(expectedQuery)).thenReturn(foundHoldings);

    var result = dcbHoldingService.findDcbEntity();

    assertThat(result).contains(dcbHolding());
  }

  @Test
  void findDcbEntity_positive_shouldReturnEmptyWhenNotExists() {
    var expectedQuery = exactMatchById(TEST_HOLDING_ID);
    when(holdingsStorageClient.findByQuery(expectedQuery)).thenReturn(ResultList.empty());
    var result = dcbHoldingService.findDcbEntity();
    assertThat(result).isEmpty();
  }

  @Test
  void createDcbEntity_positive_shouldCreateHoldingWithAllDependencies() {
    var instance = dcbInstance();
    var location = dcbLocation();
    var holdingSource = dcbHoldingSource();
    var expectedHolding = dcbHolding();

    when(dcbInstanceService.findOrCreateEntity()).thenReturn(instance);
    when(dcbLocationService.findOrCreateEntity()).thenReturn(location);
    when(dcbHoldingSourceService.findOrCreateEntity()).thenReturn(holdingSource);
    when(holdingsStorageClient.createHolding(expectedHolding)).thenReturn(expectedHolding);

    var result = dcbHoldingService.createDcbEntity();

    assertThat(result).isEqualTo(dcbHolding());
  }

  @Test
  void getDefaultValue_positive_shouldReturnHoldingWithDefaultValues() {
    var result = dcbHoldingService.getDefaultValue();
    assertThat(result).isEqualTo(dcbHolding(null));
  }

  @Test
  void findOrCreateEntity_positive_shouldReturnExistingHolding() {
    var expectedQuery = exactMatchById(TEST_HOLDING_ID);
    var holdingsResult = ResultList.asSinglePage(dcbHolding());
    when(holdingsStorageClient.findByQuery(expectedQuery)).thenReturn(holdingsResult);
    var result = dcbHoldingService.findOrCreateEntity();

    assertThat(result).isEqualTo(dcbHolding());
    verify(dcbInstanceService, never()).findOrCreateEntity();
    verify(dcbHoldingSourceService, never()).findOrCreateEntity();
    verify(dcbLocationService, never()).findOrCreateEntity();
    verify(holdingsStorageClient, never()).createHolding(any());
  }

  private static Holding dcbHolding() {
    return dcbHolding(TEST_HOLDING_SOURCE_ID);
  }

  private static Holding dcbHolding(String sourceId) {
    return Holding.builder()
      .id(TEST_HOLDING_ID)
      .instanceId(TEST_INSTANCE_ID)
      .permanentLocationId(TEST_LOCATION_ID)
      .sourceId(sourceId)
      .build();
  }

  private static InventoryInstanceDTO dcbInstance() {
    return InventoryInstanceDTO.builder()
      .id(TEST_INSTANCE_ID)
      .build();
  }

  private static HoldingSource dcbHoldingSource() {
    return HoldingSource.builder()
      .id(TEST_HOLDING_SOURCE_ID)
      .build();
  }

  private static LocationDTO dcbLocation() {
    return LocationDTO.builder()
      .id(TEST_LOCATION_ID)
      .build();
  }
}
