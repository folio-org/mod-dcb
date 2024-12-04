package org.folio.dcb.service;

import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.ItemStatus;
import org.folio.dcb.exception.StatusException;
import org.folio.dcb.service.impl.HoldingsServiceImpl;
import org.folio.dcb.service.impl.ItemServiceImpl;
import org.folio.dcb.service.impl.RequestServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.utils.EntityUtils.createDcbItem;
import static org.folio.dcb.utils.EntityUtils.createDcbPickup;
import static org.folio.dcb.utils.EntityUtils.createInventoryHolding;
import static org.folio.dcb.utils.EntityUtils.createInventoryItem;
import static org.folio.dcb.utils.EntityUtils.createUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

  @InjectMocks
  private RequestServiceImpl requestService;
  @Mock
  private ItemServiceImpl itemService;
  @Mock
  private HoldingsServiceImpl holdingsService;
  @Mock
  private CirculationClient circulationClient;

  @Test
  void createItemRequestWithPageStatusTest() {
    when(itemService.fetchItemByIdAndBarcode(any(), any())).thenReturn(createInventoryItem());
    when(holdingsService.fetchInventoryHoldingDetailsByHoldingId(any())).thenReturn(createInventoryHolding());
    requestService.createRequestBasedOnItemStatus(createUser(), createDcbItem(), createDcbPickup().getServicePointId());
    verify(itemService).fetchItemByIdAndBarcode(any(), any());
    verify(holdingsService).fetchInventoryHoldingDetailsByHoldingId(any());
    ArgumentCaptor<CirculationRequest> requestTypeCaptor = forClass(CirculationRequest.class);
    verify(circulationClient).createRequest(requestTypeCaptor.capture());
    var capturedRequest = requestTypeCaptor.getValue();
    assertEquals(CirculationRequest.RequestTypeEnum.PAGE, capturedRequest.getRequestType());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Awaiting delivery", "Paged", "In transit", "Checked out", "Awaiting pickup"})
  void createItemRequestWithHoldStatusTest(String holdStatus) {
    var inventoryItem = createInventoryItem();
    inventoryItem.setStatus(inventoryItem.getStatus().name(ItemStatus.NameEnum.fromValue(holdStatus)));
    when(itemService.fetchItemByIdAndBarcode(any(), any())).thenReturn(inventoryItem);
    when(holdingsService.fetchInventoryHoldingDetailsByHoldingId(any())).thenReturn(createInventoryHolding());
    requestService.createRequestBasedOnItemStatus(createUser(), createDcbItem(), createDcbPickup().getServicePointId());
    verify(itemService).fetchItemByIdAndBarcode(any(), any());
    verify(holdingsService).fetchInventoryHoldingDetailsByHoldingId(any());
    ArgumentCaptor<CirculationRequest> requestTypeCaptor = forClass(CirculationRequest.class);
    verify(circulationClient).createRequest(requestTypeCaptor.capture());
    var capturedRequest = requestTypeCaptor.getValue();
    assertEquals(CirculationRequest.RequestTypeEnum.HOLD, capturedRequest.getRequestType());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Aged to lost", "Claimed returned", "Declared lost", "Lost and paid",
    "Long missing", "Missing", "In process (non-requestable)", "Intellectual item", "On order",
    "Order closed", "Restricted", "Unavailable", "Unknown", "Withdrawn"})
  void createItemRequestWithInvalidStatusTest(String invalidStatus) {
    var inventoryItem = createInventoryItem();
    inventoryItem.setStatus(inventoryItem.getStatus().name(ItemStatus.NameEnum.fromValue(invalidStatus)));
    var user = createUser();
    var item = createDcbItem();
    var pickupServicePointId = createDcbPickup().getServicePointId();
    when(itemService.fetchItemByIdAndBarcode(any(), any())).thenReturn(inventoryItem);
    when(holdingsService.fetchInventoryHoldingDetailsByHoldingId(any())).thenReturn(createInventoryHolding());
    assertThrows(StatusException.class, () -> requestService.createRequestBasedOnItemStatus(user, item, pickupServicePointId));
  }

}
