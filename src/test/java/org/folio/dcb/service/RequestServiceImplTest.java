package org.folio.dcb.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.client.feign.HoldingsStorageClient;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.User;
import org.folio.dcb.service.impl.HoldingsServiceImpl;
import org.folio.dcb.service.impl.RequestServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RequestServiceImplTest {

  @Mock
  private CirculationClient circulationClient;
  @Mock
  private HoldingsServiceImpl holdingsService;

  @InjectMocks
  private RequestServiceImpl requestService;

  @Test
  void createHoldItemRequest_ShouldCreateRequest_WhenValidInput() {
    // Mock
    var randomUuid = UUID.randomUUID().toString();
    var user = User.builder().username("username").id(randomUuid).barcode("barcode").build();
    var item = DcbItem.builder().title ("title").id(randomUuid).build();
    var pickupServicePointId = "pickupPointId";

    var dcbHolding = HoldingsStorageClient.Holding.builder().id(randomUuid).build();
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(dcbHolding);

    var expectedRequest = CirculationRequest.builder().id("requestId").build();
    when(circulationClient.createRequest(any())).thenReturn(expectedRequest);

    // Act
    var request = requestService.createHoldItemRequest(user, item, pickupServicePointId);

    // Assert
    verify(holdingsService).fetchDcbHoldingOrCreateIfMissing();
    verify(circulationClient).createRequest(any());
    assertEquals(expectedRequest, request);
  }
}
