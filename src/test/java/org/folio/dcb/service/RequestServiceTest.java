package org.folio.dcb.service;

import org.folio.dcb.client.feign.RequestClient;
import org.folio.dcb.service.impl.HoldingsServiceImpl;
import org.folio.dcb.service.impl.ItemServiceImpl;
import org.folio.dcb.service.impl.RequestServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.utils.EntityUtils.createDcbItem;
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
  private RequestClient requestClient;

  @Test
  void createPageItemRequestTest() {
    when(itemService.fetchItemDetailsById(any())).thenReturn(createInventoryItem());
    when(holdingsService.fetchInventoryHoldingDetailsByHoldingId(any())).thenReturn(createInventoryHolding());
    requestService.createPageItemRequest(createUser(), createDcbItem());
    verify(itemService).fetchItemDetailsById(any());
    verify(holdingsService).fetchInventoryHoldingDetailsByHoldingId(any());
    verify(requestClient).createRequest(any());
  }

}
