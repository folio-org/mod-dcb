package org.folio.dcb.service;

import feign.FeignException;
import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.exception.CirculationRequestException;
import org.folio.dcb.service.impl.CirculationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.folio.dcb.utils.EntityUtils.createCirculationRequest;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CirculationServiceTest {

  @InjectMocks
  private CirculationServiceImpl circulationService;

  @Mock
  private CirculationClient circulationClient;

  @Mock
  private CirculationRequestService circulationRequestService;

  @Test
  void checkInByBarcodeTest(){
    circulationService.checkInByBarcode(createTransactionEntity());
    verify(circulationClient).checkInByBarcode(any());
  }

  @Test
  void checkInByBarcodeWithServicePointTest(){
    circulationService.checkInByBarcode(createTransactionEntity(), String.valueOf(UUID.randomUUID()));
    verify(circulationClient).checkInByBarcode(any());
  }

  @Test
  void cancelRequestTest() {
    when(circulationRequestService.getCancellationRequestIfOpenOrNull(anyString())).thenReturn(createCirculationRequest());
    circulationService.cancelRequest(createTransactionEntity(), false);
    verify(circulationClient).cancelRequest(anyString(), any());
  }

  @Test
  void shouldThrowExceptionWhenRequestIsNotUpdated() {
    when(circulationRequestService.getCancellationRequestIfOpenOrNull(anyString())).thenReturn(createCirculationRequest());
    when(circulationClient.cancelRequest(anyString(), any())).thenThrow(FeignException.BadRequest.class);
    assertThrows(CirculationRequestException.class, () -> circulationService.cancelRequest(createTransactionEntity(), false));
  }
  @Test
  void shouldUpdateRequestApiWithIsDcbRerequestCancellationTrue() {
    CirculationRequest fetchedRequest = createCirculationRequest();
    fetchedRequest.setIsDcbReRequestCancellation(null);
    CirculationRequest requestToBeCancelled = createCirculationRequest();
    requestToBeCancelled.setIsDcbReRequestCancellation(true);
    when(circulationRequestService.getCancellationRequestIfOpenOrNull(anyString())).thenReturn(fetchedRequest);
    circulationService.cancelRequest(createTransactionEntity(), true);
  }
}
