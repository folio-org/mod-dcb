package org.folio.dcb.service;

import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.service.impl.HoldingsServiceImpl;
import org.folio.dcb.service.impl.ItemServiceImpl;
import org.folio.dcb.service.impl.RequestServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.utils.EntityUtils.createDcbItem;
import static org.folio.dcb.utils.EntityUtils.createDcbPickup;
import static org.folio.dcb.utils.EntityUtils.createInventoryHolding;
import static org.folio.dcb.utils.EntityUtils.createInventoryItem;
import static org.folio.dcb.utils.EntityUtils.createUser;
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
  void createPageItemRequestTest() {
    when(itemService.fetchItemByIdAndBarcode(any(), any())).thenReturn(createInventoryItem());
    when(holdingsService.fetchInventoryHoldingDetailsByHoldingId(any())).thenReturn(createInventoryHolding());
    requestService.createPageItemRequest(createUser(), createDcbItem(), createDcbPickup().getServicePointId());
    verify(itemService).fetchItemByIdAndBarcode(any(), any());
    verify(holdingsService).fetchInventoryHoldingDetailsByHoldingId(any());
    verify(circulationClient).createRequest(any());
  }

}
