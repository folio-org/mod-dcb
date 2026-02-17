package org.folio.dcb.service;

import org.folio.dcb.integration.circstorage.CancellationReasonClient;
import org.folio.dcb.integration.circstorage.CirculationRequestClient;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.service.entities.DcbEntityServiceFacade;
import org.folio.dcb.service.impl.CirculationRequestServiceImpl;
import org.folio.dcb.integration.circulation.model.RequestStatus;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.utils.EntityUtils.createCirculationRequest;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CirculationRequestServiceTests {

  private static final String REQUEST_ID = UUID.randomUUID().toString();

  @InjectMocks private CirculationRequestServiceImpl circulationRequestService;
  @Mock private CirculationRequestClient circulationStorageClient;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private DcbEntityServiceFacade dcbEntityServiceFacade;

  @Test
  void getCancellationRequestIfOpenOrNullTest() {
    CirculationRequest openRequest = createCirculationRequest();
    openRequest.setStatus(RequestStatus.OPEN_AWAITING_PICKUP.getValue());
    when(circulationStorageClient.fetchRequestById(anyString())).thenReturn(openRequest);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    var defaultCancellationReasont = new CancellationReasonClient.CancellationReason();
    defaultCancellationReasont.setId(UUID.randomUUID().toString());
    when(dcbEntityServiceFacade.findOrCreateCancellationReason()).thenReturn(defaultCancellationReasont);
    var cancelRequest = circulationRequestService.getCancellationRequestIfOpenOrNull(anyString());
    Assertions.assertEquals(RequestStatus.CLOSED_CANCELLED, RequestStatus.from(cancelRequest.getStatus()));
  }

  @Test
  void getCancellationRequestIfOpenOrNull_positive_notOpenRequestStatus() {
    var circulationRequest = mock(CirculationRequest.class);
    when(circulationRequest.getStatus()).thenReturn(RequestStatus.CLOSED_UNFILLED.getValue());
    when(circulationStorageClient.fetchRequestById(REQUEST_ID)).thenReturn(circulationRequest);

    var result = circulationRequestService.getCancellationRequestIfOpenOrNull(REQUEST_ID);

    assertThat(result).isNull();
  }

  @Test
  void fetchRequestById_positive() {
    var circulationRequest = createCirculationRequest();
    when(circulationStorageClient.fetchRequestById(REQUEST_ID)).thenReturn(circulationRequest);
    var result = circulationRequestService.fetchRequestById(REQUEST_ID);

    assertThat(result).isSameAs(circulationRequest);
  }

  @Test
  void fetchRequestById_negative_notFound() {
    when(circulationStorageClient.fetchRequestById(REQUEST_ID)).thenThrow(HttpClientErrorException.NotFound.class);
    var result = circulationRequestService.fetchRequestById(REQUEST_ID);

    assertThat(result).isNull();
  }
}
