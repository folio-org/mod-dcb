package org.folio.dcb.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.UUID;
import org.folio.dcb.client.feign.CirculationItemClient;
import org.folio.dcb.client.feign.HoldingsStorageClient;
import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.CirculationItemCollection;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.service.impl.CirculationItemServiceImpl;
import org.folio.dcb.service.impl.HoldingsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class CirculationItemServiceImplTest {

  @Mock
  private CirculationItemClient circulationItemClient;
  @Mock
  private HoldingsServiceImpl holdingsService;
  @Mock
  private ItemService itemService;

  @InjectMocks
  private CirculationItemServiceImpl circulationItemService;

  @Test
  void checkIfItemExistsAndCreate_ShouldReturnExistingItem_WhenItemExists() {
    // Mock
    var randomUuid = UUID.randomUUID().toString();
    var dcbItem = DcbItem.builder().barcode("barcode123").title("title").id(randomUuid).build();
    var pickupServicePointId = "pickupPointId";

    var existingItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.fetchItemByCqlQuery(any()))
      .thenReturn(CirculationItemCollection.builder()
        .items(Collections.singletonList(existingItem))
        .build());

    // Act
    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId);

    // Assert
    verify(circulationItemClient).fetchItemByCqlQuery(any());
    assertEquals(existingItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_ShouldCreateNewItem_WhenItemDoesNotExist() {
    // Mock
    var randomUuid = UUID.randomUUID().toString();
    var dcbItem = DcbItem.builder().barcode("barcode123").title("title").id(randomUuid).build();
    var pickupServicePointId = "pickupPointId";

    when(circulationItemClient.fetchItemByCqlQuery(any()))
      .thenReturn(CirculationItemCollection.builder().items(Collections.emptyList()).build());

    var dcbHolding = HoldingsStorageClient.Holding.builder().id(randomUuid).build();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);

    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    // Act
    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId);

    // Assert
    verify(circulationItemClient).fetchItemByCqlQuery(any());
    verify(holdingsService).fetchDcbHoldingOrCreateIfMissing();
    verify(circulationItemClient).createCirculationItem(any(), any());
    assertEquals(createdItem, result);
  }
}
