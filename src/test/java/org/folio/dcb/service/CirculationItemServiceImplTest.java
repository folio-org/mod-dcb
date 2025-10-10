package org.folio.dcb.service;

import static org.folio.dcb.client.feign.LocationsClient.LocationDTO;
import static org.folio.spring.model.ResultList.asSinglePage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;
import org.folio.dcb.client.feign.CirculationItemClient;
import org.folio.dcb.client.feign.HoldingsStorageClient.Holding;
import org.folio.dcb.client.feign.LocationUnitClient;
import org.folio.dcb.client.feign.LocationUnitClient.LocationUnit;
import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.config.DcbHubProperties;
import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.CirculationItemCollection;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.service.impl.CirculationItemServiceImpl;
import org.folio.dcb.service.impl.HoldingsServiceImpl;
import org.folio.dcb.utils.DCBConstants;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CirculationItemServiceImplTest {

  private static final String TEST_HOLDING_ID = randomUuid();
  private static final String TEST_LOCATION_ID = randomUuid();
  private static final String TEST_CIRCULATION_ITEM_ID = randomUuid();
  private static final String TEST_SERVICE_POINT_ID = randomUuid();
  private static final String TEST_LENDING_LIBRARY_CODE = "TST";
  private static final String TEST_DCB_LOCATION_CODE = "TST";

  @Mock private CirculationItemClient circulationItemClient;
  @Mock private HoldingsServiceImpl holdingsService;
  @Mock private ItemService itemService;
  @Mock private LocationsClient locationsClient;
  @Mock private DcbHubProperties dcbHubProperties;
  @Mock private LocationUnitClient locationUnitClient;
  @InjectMocks private CirculationItemServiceImpl circulationItemService;

  @Test
  void checkIfItemExistsAndCreate_ShouldReturnExistingItem_WhenItemExists() {
    var existingItem = circulationItem();

    when(circulationItemClient.fetchItemByCqlQuery(any()))
      .thenReturn(CirculationItemCollection.builder()
        .items(Collections.singletonList(existingItem))
        .build());

    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem(), TEST_SERVICE_POINT_ID);

    verify(circulationItemClient).fetchItemByCqlQuery(any());
    assertEquals(existingItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_ShouldCreateNewItem_WhenItemDoesNotExist() {
    var randomUuid = randomUuid();

    when(dcbHubProperties.isFetchDcbLocationsEnabled()).thenReturn(false);
    when(circulationItemClient.fetchItemByCqlQuery(any())).thenReturn(emptyCirculationItems());
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding());
    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = circulationItem();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem(), TEST_SERVICE_POINT_ID);

    verify(circulationItemClient).fetchItemByCqlQuery(any());
    verify(holdingsService).fetchDcbHoldingOrCreateIfMissing();
    verify(circulationItemClient).createCirculationItem(any(), any());
    assertEquals(createdItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_ShouldFetchShadowLocation_positive() {
    var randomUuid = randomUuid();
    var dcbItem = dcbItem(TEST_DCB_LOCATION_CODE, null);
    var pickupServicePointId = "pickupPointId";

    when(dcbHubProperties.isFetchDcbLocationsEnabled()).thenReturn(true);
    when(circulationItemClient.fetchItemByCqlQuery(any())).thenReturn(emptyCirculationItems());

    var dcbHolding = dcbHolding();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);
    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    LocationDTO locationDTO = testLocation();
    when(locationsClient.findLocationByQuery(String.format("code==\"%s\"", TEST_DCB_LOCATION_CODE), true, 1, 0))
      .thenReturn(asSinglePage(locationDTO));

    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId);

    ArgumentCaptor<CirculationItem> circulationItemArgumentCaptor = ArgumentCaptor.forClass(CirculationItem.class);
    verify(circulationItemClient).createCirculationItem(any(), circulationItemArgumentCaptor.capture());
    CirculationItem capturedItem = circulationItemArgumentCaptor.getValue();
    assertEquals(TEST_LOCATION_ID, capturedItem.getEffectiveLocationId());
    assertEquals(createdItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_NoShadowLocationFound_negative() {
    var randomUuid = randomUuid();
    var dcbItem = dcbItem(TEST_DCB_LOCATION_CODE, null);

    when(dcbHubProperties.isFetchDcbLocationsEnabled()).thenReturn(true);
    when(circulationItemClient.fetchItemByCqlQuery(any())).thenReturn(emptyCirculationItems());

    var dcbHolding = Holding.builder().id(randomUuid).build();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);

    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    var expectedQuery = String.format("code==\"%s\"", TEST_DCB_LOCATION_CODE);
    when(locationsClient.findLocationByQuery(expectedQuery, true, 1, 0)).thenReturn(ResultList.empty());

    // Act
    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, TEST_SERVICE_POINT_ID);

    // Assert
    ArgumentCaptor<CirculationItem> circulationItemArgumentCaptor = ArgumentCaptor.forClass(CirculationItem.class);
    verify(circulationItemClient).createCirculationItem(any(), circulationItemArgumentCaptor.capture());
    CirculationItem capturedItem = circulationItemArgumentCaptor.getValue();
    assertEquals(DCBConstants.LOCATION_ID, capturedItem.getEffectiveLocationId());
    assertEquals(createdItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_NonExistedShadowLocationCode_negative() {
    // Mock
    var randomUuid = randomUuid();
    var locationCode = "nonExistedShadowLocationCode";
    var dcbItem = DcbItem.builder().barcode("barcode123").title("title").id(randomUuid).locationCode(locationCode).build();
    var pickupServicePointId = "pickupPointId";

    when(dcbHubProperties.isFetchDcbLocationsEnabled()).thenReturn(true);
    when(circulationItemClient.fetchItemByCqlQuery(any())).thenReturn(emptyCirculationItems());

    var dcbHolding = Holding.builder().id(randomUuid).build();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);

    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    when(locationsClient.findLocationByQuery("code==\"nonExistedShadowLocationCode\"", true, 1, 0))
      .thenReturn(ResultList.of(0, Collections.emptyList()));

    // Act
    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId);

    // Assert
    ArgumentCaptor<CirculationItem> circulationItemArgumentCaptor = ArgumentCaptor.forClass(CirculationItem.class);
    verify(circulationItemClient).createCirculationItem(any(), circulationItemArgumentCaptor.capture());
    CirculationItem capturedItem = circulationItemArgumentCaptor.getValue();
    assertEquals(DCBConstants.LOCATION_ID, capturedItem.getEffectiveLocationId());
    assertEquals(createdItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_NullLocationCode_negative() {
    var randomUuid = randomUuid();
    String locationCode = null;
    var dcbItem = DcbItem.builder().barcode("barcode123").title("title").id(randomUuid).locationCode(locationCode).build();
    var pickupServicePointId = "pickupPointId";

    when(dcbHubProperties.isFetchDcbLocationsEnabled()).thenReturn(true);
    when(circulationItemClient.fetchItemByCqlQuery(any())).thenReturn(emptyCirculationItems());

    var dcbHolding = Holding.builder().id(randomUuid).build();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);

    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId);

    // Assert
    ArgumentCaptor<CirculationItem> circulationItemArgumentCaptor = ArgumentCaptor.forClass(CirculationItem.class);
    verify(circulationItemClient).createCirculationItem(any(), circulationItemArgumentCaptor.capture());
    CirculationItem capturedItem = circulationItemArgumentCaptor.getValue();
    assertEquals(DCBConstants.LOCATION_ID, capturedItem.getEffectiveLocationId());
    assertEquals(createdItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_ShadowLocationLookupDisabled_negative() {
    String locationCode = "shadowLocationCode";
    var randomUuid = randomUuid();
    var dcbItem = DcbItem.builder().barcode("barcode123").title("title").id(randomUuid).locationCode(locationCode).build();
    var pickupServicePointId = "pickupPointId";

    when(dcbHubProperties.isFetchDcbLocationsEnabled()).thenReturn(false);
    when(circulationItemClient.fetchItemByCqlQuery(any())).thenReturn(emptyCirculationItems());

    var dcbHolding = Holding.builder().id(randomUuid).build();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);

    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId);

    ArgumentCaptor<CirculationItem> circulationItemArgumentCaptor = ArgumentCaptor.forClass(CirculationItem.class);
    verify(circulationItemClient).createCirculationItem(any(), circulationItemArgumentCaptor.capture());
    CirculationItem capturedItem = circulationItemArgumentCaptor.getValue();
    assertEquals(DCBConstants.LOCATION_ID, capturedItem.getEffectiveLocationId());
    assertEquals(createdItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_positive_lendingLibraryCode() {
    var randomUuid = randomUuid();
    var pickupServicePointId = "pickupPointId";

    when(dcbHubProperties.isFetchDcbLocationsEnabled()).thenReturn(true);
    when(circulationItemClient.fetchItemByCqlQuery(any())).thenReturn(emptyCirculationItems());

    var dcbHolding = Holding.builder().id(randomUuid).build();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);

    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    var dcbItem = dcbItem(null, TEST_LENDING_LIBRARY_CODE);
    when(locationUnitClient.findLibrariesByQuery("code==\"TST\"", true, 1, 0))
      .thenReturn(asSinglePage(testLibraryUnit()));
    when(locationsClient.findLocationByQuery("libraryId==\"" + TEST_LOCATION_ID + "\"", true, 1, 0))
      .thenReturn(asSinglePage(testLocation()));

    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId);

    ArgumentCaptor<CirculationItem> circulationItemArgumentCaptor = ArgumentCaptor.forClass(CirculationItem.class);
    verify(circulationItemClient).createCirculationItem(any(), circulationItemArgumentCaptor.capture());
    CirculationItem capturedItem = circulationItemArgumentCaptor.getValue();
    assertEquals(TEST_LOCATION_ID, capturedItem.getEffectiveLocationId());
    assertEquals(createdItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_positive_lendingLibraryNotFoundByCode() {
    var randomUuid = randomUuid();
    var pickupServicePointId = "pickupPointId";

    when(dcbHubProperties.isFetchDcbLocationsEnabled()).thenReturn(true);
    when(circulationItemClient.fetchItemByCqlQuery(any())).thenReturn(emptyCirculationItems());

    var dcbHolding = Holding.builder().id(randomUuid).build();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);

    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    var dcbItem = dcbItem(null, TEST_LENDING_LIBRARY_CODE);
    when(locationUnitClient.findLibrariesByQuery("code==\"TST\"", true, 1, 0)).thenReturn(ResultList.empty());

    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId);

    ArgumentCaptor<CirculationItem> circulationItemArgumentCaptor = ArgumentCaptor.forClass(CirculationItem.class);
    verify(circulationItemClient).createCirculationItem(any(), circulationItemArgumentCaptor.capture());
    CirculationItem capturedItem = circulationItemArgumentCaptor.getValue();
    assertEquals(DCBConstants.LOCATION_ID, capturedItem.getEffectiveLocationId());
    assertEquals(createdItem, result);
  }

  @Test
  void checkIfItemExistsAndCreate_positive_locationNotFoundByLibraryId() {
    var randomUuid = randomUuid();
    var pickupServicePointId = "pickupPointId";

    when(dcbHubProperties.isFetchDcbLocationsEnabled()).thenReturn(true);
    when(circulationItemClient.fetchItemByCqlQuery(any())).thenReturn(emptyCirculationItems());

    var dcbHolding = Holding.builder().id(randomUuid).build();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);

    when(itemService.fetchItemMaterialTypeIdByMaterialTypeName(any())).thenReturn(randomUuid);

    var createdItem = CirculationItem.builder().id(randomUuid).barcode("barcode123").build();
    when(circulationItemClient.createCirculationItem(any(), any())).thenReturn(createdItem);

    var dcbItem = dcbItem(null, TEST_LENDING_LIBRARY_CODE);
    when(locationUnitClient.findLibrariesByQuery("code==\"TST\"", true, 1, 0))
      .thenReturn(asSinglePage(testLibraryUnit()));
    when(locationsClient.findLocationByQuery("libraryId==\"" + TEST_LOCATION_ID + "\"", true, 1, 0))
      .thenReturn(ResultList.empty());

    var result = circulationItemService.checkIfItemExistsAndCreate(dcbItem, pickupServicePointId);

    ArgumentCaptor<CirculationItem> circulationItemArgumentCaptor = ArgumentCaptor.forClass(CirculationItem.class);
    verify(circulationItemClient).createCirculationItem(any(), circulationItemArgumentCaptor.capture());
    CirculationItem capturedItem = circulationItemArgumentCaptor.getValue();
    assertEquals(DCBConstants.LOCATION_ID, capturedItem.getEffectiveLocationId());
    assertEquals(createdItem, result);
  }

  private static LocationDTO testLocation() {
    return LocationDTO.builder()
      .id(TEST_LOCATION_ID)
      .name("locationName")
      .code(TEST_DCB_LOCATION_CODE)
      .build();
  }

  private static LocationUnit testLibraryUnit() {
    return LocationUnit.builder()
      .id(TEST_LOCATION_ID)
      .name("Test Library")
      .code(TEST_DCB_LOCATION_CODE)
      .build();
  }

  private static String randomUuid() {
    return UUID.randomUUID().toString();
  }

  private static CirculationItemCollection emptyCirculationItems() {
    return CirculationItemCollection.builder().items(Collections.emptyList()).build();
  }

  private static CirculationItem circulationItem() {
    return CirculationItem.builder()
      .id(TEST_CIRCULATION_ITEM_ID)
      .barcode("barcode123")
      .build();
  }

  private static DcbItem dcbItem() {
    return dcbItem(null, null);
  }

  private static DcbItem dcbItem(String locationCode, String lendingLibraryCode) {
    return DcbItem.builder()
      .barcode("barcode123")
      .title("title")
      .id(randomUuid())
      .locationCode(locationCode)
      .lendingLibraryCode(lendingLibraryCode)
      .build();
  }

  private static Holding dcbHolding() {
    return Holding.builder().id(TEST_HOLDING_ID).build();
  }
}
