package org.folio.dcb.service;

import feign.FeignException;
import org.folio.dcb.client.feign.InventoryHoldingsStorageClient;
import org.folio.dcb.service.impl.HoldingsServiceImpl;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.folio.dcb.utils.EntityUtils.createInventoryHolding;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryHoldingsServiceTest {

  @InjectMocks
  private HoldingsServiceImpl holdingsService;
  @Mock
  private InventoryHoldingsStorageClient holdingsStorageClient;

  @Test
  void fetchInventoryHoldingDetailsByIdTest() {
    var inventoryHolding = createInventoryHolding();
    var holdingId = UUID.randomUUID().toString();
    when(holdingsStorageClient.findHolding(any())).thenReturn(inventoryHolding);
    var response = holdingsService.fetchInventoryHoldingDetailsByHoldingId(holdingId);
    verify(holdingsStorageClient).findHolding(holdingId);
    assertEquals(inventoryHolding, response);
  }

  @Test
  void fetchInventoryHoldingDetailsByInvalidIdTest() {
    var holdingId = UUID.randomUUID().toString();
    doThrow(FeignException.NotFound.class).when(holdingsStorageClient).findHolding(holdingId);
    assertThrows(NotFoundException.class, () -> holdingsService.fetchInventoryHoldingDetailsByHoldingId(holdingId));
  }

}
