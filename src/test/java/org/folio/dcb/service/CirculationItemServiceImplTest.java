package org.folio.dcb.service;

import static org.folio.dcb.client.feign.LocationsClient.LocationDTO;
import static org.folio.dcb.utils.DCBConstants.LOCATION_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.dcb.client.feign.CirculationItemClient;
import org.folio.dcb.client.feign.HoldingsStorageClient;
import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.config.DcbHubProperties;
import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.CirculationItemCollection;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.service.impl.CirculationItemServiceImpl;
import org.folio.dcb.service.impl.HoldingsServiceImpl;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CirculationItemServiceImplTest {

  @Mock
  private CirculationItemClient circulationItemClient;
  @Mock
  private HoldingsServiceImpl holdingsService;
  @Mock
  private ItemService itemService;
  @Mock
  private LocationsClient locationsClient;

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
    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId, dcbItem.getLocationCode());

    // Assert
    verify(circulationItemClient).fetchItemByCqlQuery(any());
    assertEquals(existingItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_ShouldCreateNewItem_WhenItemDoesNotExist() {
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(false);
    ReflectionTestUtils.setField(circulationItemService,  "dcbHubProperties", dcbHubProperties);

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
    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId, dcbItem.getLocationCode());

    // Assert
    verify(circulationItemClient).fetchItemByCqlQuery(any());
    verify(holdingsService).fetchDcbHoldingOrCreateIfMissing();
    verify(circulationItemClient).createCirculationItem(any(), any());
    assertEquals(createdItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_ShouldFetchShadowLocation_positive() {
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(true);
    ReflectionTestUtils.setField(circulationItemService,  "dcbHubProperties", dcbHubProperties);

    // Mock
    String locationCode = "shadowLocationCode";
    var randomUuid = UUID.randomUUID().toString();
    var dcbItem = DcbItem.builder().barcode("barcode123").title("title").id(randomUuid).locationCode(locationCode).build();
    var pickupServicePointId = "pickupPointId";

    when(circulationItemClient.fetchItemByCqlQuery(any()))
      .thenReturn(CirculationItemCollection.builder().items(Collections.emptyList()).build());

    var dcbHolding = HoldingsStorageClient.Holding.builder().id(randomUuid).build();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);

    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    String mockEffectiveLocationId = UUID.randomUUID().toString();
    LocationDTO locationDTO = LocationDTO.builder()
      .id(mockEffectiveLocationId).name("locationName").code(locationCode).build();
    when(locationsClient.findLocationByQuery(String.format("code==%s", locationCode), true, 1, 0))
      .thenReturn(ResultList.of(1, List.of(locationDTO)));

    // Act
    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId, dcbItem.getLocationCode());

    // Assert
    ArgumentCaptor<CirculationItem> circulationItemArgumentCaptor = ArgumentCaptor.forClass(CirculationItem.class);
    verify(circulationItemClient).createCirculationItem(any(), circulationItemArgumentCaptor.capture());
    CirculationItem capturedItem = circulationItemArgumentCaptor.getValue();
    assertEquals(mockEffectiveLocationId, capturedItem.getEffectiveLocationId());
    assertEquals(createdItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_NoShadowLocationFound_negative() {
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(true);
    ReflectionTestUtils.setField(circulationItemService,  "dcbHubProperties", dcbHubProperties);

    // Mock
    String locationCode = "shadowLocationCode";
    var randomUuid = UUID.randomUUID().toString();
    var dcbItem = DcbItem.builder().barcode("barcode123").title("title").id(randomUuid).locationCode(locationCode).build();
    var pickupServicePointId = "pickupPointId";

    when(circulationItemClient.fetchItemByCqlQuery(any()))
      .thenReturn(CirculationItemCollection.builder().items(Collections.emptyList()).build());

    var dcbHolding = HoldingsStorageClient.Holding.builder().id(randomUuid).build();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);

    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    when(locationsClient.findLocationByQuery(String.format("code==%s", locationCode), true, 1, 0))
      .thenReturn(ResultList.of(0, Collections.emptyList()));

    // Act
    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId, dcbItem.getLocationCode());

    // Assert
    ArgumentCaptor<CirculationItem> circulationItemArgumentCaptor = ArgumentCaptor.forClass(CirculationItem.class);
    verify(circulationItemClient).createCirculationItem(any(), circulationItemArgumentCaptor.capture());
    CirculationItem capturedItem = circulationItemArgumentCaptor.getValue();
    assertEquals(LOCATION_ID, capturedItem.getEffectiveLocationId());
    assertEquals(createdItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_NonExistedShadowLocationCode_negative() {
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(true);
    ReflectionTestUtils.setField(circulationItemService,  "dcbHubProperties", dcbHubProperties);
    // Mock
    var randomUuid = UUID.randomUUID().toString();
    var locationCode = "nonExistedShadowLocationCode";
    var dcbItem = DcbItem.builder().barcode("barcode123").title("title").id(randomUuid).locationCode(locationCode).build();
    var pickupServicePointId = "pickupPointId";

    when(circulationItemClient.fetchItemByCqlQuery(any()))
      .thenReturn(CirculationItemCollection.builder().items(Collections.emptyList()).build());

    var dcbHolding = HoldingsStorageClient.Holding.builder().id(randomUuid).build();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);

    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    when(locationsClient.findLocationByQuery("code==nonExistedShadowLocationCode", true, 1, 0))
      .thenReturn(ResultList.of(0, Collections.emptyList()));

    // Act
    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId, dcbItem.getLocationCode());

    // Assert
    ArgumentCaptor<CirculationItem> circulationItemArgumentCaptor = ArgumentCaptor.forClass(CirculationItem.class);
    verify(circulationItemClient).createCirculationItem(any(), circulationItemArgumentCaptor.capture());
    CirculationItem capturedItem = circulationItemArgumentCaptor.getValue();
    assertEquals(LOCATION_ID, capturedItem.getEffectiveLocationId());
    assertEquals(createdItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_NullLocationCode_negative() {
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(true);
    ReflectionTestUtils.setField(circulationItemService,  "dcbHubProperties", dcbHubProperties);
    // Mock
    var randomUuid = UUID.randomUUID().toString();
    String locationCode = null;
    var dcbItem = DcbItem.builder().barcode("barcode123").title("title").id(randomUuid).locationCode(locationCode).build();
    var pickupServicePointId = "pickupPointId";

    when(circulationItemClient.fetchItemByCqlQuery(any()))
      .thenReturn(CirculationItemCollection.builder().items(Collections.emptyList()).build());

    var dcbHolding = HoldingsStorageClient.Holding.builder().id(randomUuid).build();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);

    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    // Act
    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId, dcbItem.getLocationCode());

    // Assert
    ArgumentCaptor<CirculationItem> circulationItemArgumentCaptor = ArgumentCaptor.forClass(CirculationItem.class);
    verify(circulationItemClient).createCirculationItem(any(), circulationItemArgumentCaptor.capture());
    CirculationItem capturedItem = circulationItemArgumentCaptor.getValue();
    assertEquals(LOCATION_ID, capturedItem.getEffectiveLocationId());
    assertEquals(createdItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_ShadowLocationLookupDisabled_negative() {
    DcbHubProperties dcbHubProperties = new DcbHubProperties();
    dcbHubProperties.setFetchDcbLocationsEnabled(false);
    ReflectionTestUtils.setField(circulationItemService,  "dcbHubProperties", dcbHubProperties);
    // Mock
    String locationCode = "shadowLocationCode";
    var randomUuid = UUID.randomUUID().toString();
    var dcbItem = DcbItem.builder().barcode("barcode123").title("title").id(randomUuid).locationCode(locationCode).build();
    var pickupServicePointId = "pickupPointId";

    when(circulationItemClient.fetchItemByCqlQuery(any()))
      .thenReturn(CirculationItemCollection.builder().items(Collections.emptyList()).build());

    var dcbHolding = HoldingsStorageClient.Holding.builder().id(randomUuid).build();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);

    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    // Act
    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId, dcbItem.getLocationCode());

    // Assert
    ArgumentCaptor<CirculationItem> circulationItemArgumentCaptor = ArgumentCaptor.forClass(CirculationItem.class);
    verify(circulationItemClient).createCirculationItem(any(), circulationItemArgumentCaptor.capture());
    CirculationItem capturedItem = circulationItemArgumentCaptor.getValue();
    assertEquals(LOCATION_ID, capturedItem.getEffectiveLocationId());
    assertEquals(createdItem, result);
  }
}
