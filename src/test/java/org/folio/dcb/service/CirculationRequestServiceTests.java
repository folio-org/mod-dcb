package org.folio.dcb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.utils.EntityUtils.createCirculationRequest;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.integration.circstorage.CancellationReasonClient;
import org.folio.dcb.integration.circstorage.CirculationRequestClient;
import org.folio.dcb.integration.circulation.model.RequestStatus;
import org.folio.dcb.service.entities.DcbEntityServiceFacade;
import org.folio.dcb.service.impl.CirculationRequestServiceImpl;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;

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

  @ParameterizedTest
  @ValueSource(strings = {"Open - Not yet filled", "Open - In transit", "Open - Awaiting delivery"})
  void getCancellationRequestIfOpenOrNull_positive_differentOpenStatuses(String openStatus) {
    // TestMate-60a75c2b5853a535ea01adc9980310e2
    var openRequest = createCirculationRequest().status(openStatus);
    when(circulationStorageClient.fetchRequestById(REQUEST_ID)).thenReturn(openRequest);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    var cancellationReason = new CancellationReasonClient.CancellationReason();
    cancellationReason.setId(UUID.fromString("00000000-0000-0000-0000-000000000002").toString());
    when(dcbEntityServiceFacade.findOrCreateCancellationReason()).thenReturn(cancellationReason);

    var result = circulationRequestService.getCancellationRequestIfOpenOrNull(REQUEST_ID);

    assertThat(RequestStatus.from(result.getStatus())).isEqualTo(RequestStatus.CLOSED_CANCELLED);
    verify(circulationStorageClient).fetchRequestById(REQUEST_ID);
    verify(dcbEntityServiceFacade).findOrCreateCancellationReason();
  }
}
