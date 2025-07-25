package org.folio.dcb.service;

import static org.folio.dcb.utils.DCBConstants.HOLDING_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import org.folio.dcb.client.feign.HoldingSourcesClient;
import org.folio.dcb.client.feign.HoldingsStorageClient;
import org.folio.dcb.exception.InventoryResourceOperationException;
import org.folio.dcb.service.impl.HoldingsServiceImpl;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HoldingsServiceImplTest {

  @Mock
  private HoldingsStorageClient holdingsStorageClient;
  @Mock
  private HoldingSourcesClient holdingSourcesClient;

  @InjectMocks
  private HoldingsServiceImpl holdingsService;

  @Test
  void fetchDcbHoldingOrCreateIfMissing_ShouldFetchHolding_WhenFound() {
    // Mock
    var expected = HoldingsStorageClient.Holding.builder().build();
    when(holdingsStorageClient.findHolding(HOLDING_ID)).thenReturn(expected);

    // Act
    var holding = holdingsService.fetchDcbHoldingOrCreateIfMissing();

    // Assert
    verify(holdingsStorageClient).findHolding(HOLDING_ID);
    assertEquals(expected, holding);
  }

  @Test
  void fetchDcbHoldingOrCreateIfMissing_ShouldCreateHolding_WhenNotFound() {
    // Mock
    when(holdingsStorageClient.findHolding(HOLDING_ID))
      .thenThrow(FeignException.NotFound.class);
    var created = HoldingsStorageClient.Holding.builder().id(HOLDING_ID).build();
    when(holdingsStorageClient.createHolding(any())).thenReturn(created);

    // Also mock holdingSourcesClient for createHolding() path
    var holdingSource = HoldingSourcesClient.HoldingSource.builder().id("sourceId").build();
    when(holdingSourcesClient.querySourceByName(any())).thenReturn(ResultList.asSinglePage(holdingSource));

    // Act
    var holding = holdingsService.fetchDcbHoldingOrCreateIfMissing();

    // Assert
    assertEquals(created, holding);
  }

  @Test
  void fetchDcbHoldingOrCreateIfMissing_ShouldThrowException_WhenCreateHoldingFails() {
    // Mock
    when(holdingsStorageClient.findHolding(HOLDING_ID))
      .thenThrow(FeignException.NotFound.class);
    doThrow(new RuntimeException("error")).when(holdingsStorageClient).createHolding(any());

    // Also mock holdingSourcesClient for createHolding() path
    var holdingSource = HoldingSourcesClient.HoldingSource.builder().id("sourceId").build();
    when(holdingSourcesClient.querySourceByName(any())).thenReturn(ResultList.asSinglePage(holdingSource));

    // Act & Assert
    assertThrows(InventoryResourceOperationException.class,
      () -> holdingsService.fetchDcbHoldingOrCreateIfMissing());
  }

  @Test
  void fetchDcbHoldingOrCreateIfMissing_ShouldCreateHolding_WhenHoldingSourceNotFound() {
    // Mock
    when(holdingsStorageClient.findHolding(HOLDING_ID))
      .thenThrow(FeignException.NotFound.class);
    var created = HoldingsStorageClient.Holding.builder().id(HOLDING_ID).build();
    when(holdingsStorageClient.createHolding(any())).thenReturn(created);

    // Also mock holdingSourcesClient for createHolding() path
    var holdingSource = HoldingSourcesClient.HoldingSource.builder().id("sourceId").build();
    when(holdingSourcesClient.querySourceByName(any())).thenReturn(ResultList.empty());
    when(holdingSourcesClient.createHoldingsRecordSource(any()))
      .thenReturn(holdingSource);

    // Act
    var holding = holdingsService.fetchDcbHoldingOrCreateIfMissing();

    // Assert
    assertEquals(created, holding);
  }
}
