package org.folio.dcb.service;

import org.folio.dcb.client.feign.CirculationRequestClient;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.service.impl.CirculationRequestServiceImpl;
import org.folio.dcb.utils.RequestStatus;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.folio.dcb.utils.EntityUtils.createCirculationRequest;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CirculationRequestServiceTests {

  @InjectMocks
  private CirculationRequestServiceImpl circulationRequestService;

  @Mock
  private CirculationRequestClient circulationStorageClient;

  @Mock
  private FolioExecutionContext folioExecutionContext;

  @Test
  void getCancellationRequestIfOpenOrNullTest() {
    CirculationRequest openRequest = createCirculationRequest();
    openRequest.setStatus(RequestStatus.OPEN_AWAITING_PICKUP.getValue());
    when(circulationStorageClient.fetchRequestById(anyString())).thenReturn(openRequest);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    var cancelRequest = circulationRequestService.getCancellationRequestIfOpenOrNull(anyString());
    Assertions.assertEquals(RequestStatus.CLOSED_CANCELLED, RequestStatus.from(cancelRequest.getStatus()));
  }
}
