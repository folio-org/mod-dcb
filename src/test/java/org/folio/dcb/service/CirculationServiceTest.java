package org.folio.dcb.service;

import static java.util.UUID.randomUUID;
import static org.folio.dcb.utils.EntityUtils.createCirculationRequest;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.ClaimedReturnedResolution;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.exception.CirculationRequestException;
import org.folio.dcb.integration.circulation.CirculationClient;
import org.folio.dcb.service.impl.CirculationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class CirculationServiceTest {

  @InjectMocks private CirculationServiceImpl circulationService;
  @Mock private CirculationClient circulationClient;
  @Mock private CirculationRequestService circulationRequestService;

  @Test
  void checkInByBarcodeTest() {
    circulationService.checkInByBarcode(createTransactionEntity());
    verify(circulationClient).checkInByBarcode(any());
  }

  @Test
  void checkInByBarcodeWithServicePointTest() {
    circulationService.checkInByBarcode(createTransactionEntity(), String.valueOf(UUID.randomUUID()));
    verify(circulationClient).checkInByBarcode(any());
  }

  @Test
  void checkInByBarcodeWithServicePointAndClaimedReturnedResolutionTest() {
    circulationService.checkInByBarcode(createTransactionEntity(), randomUUID().toString(),
      ClaimedReturnedResolution.FOUND_BY_LIBRARY);
    verify(circulationClient).checkInByBarcode(argThat(req ->
      ClaimedReturnedResolution.FOUND_BY_LIBRARY.equals(req.getClaimedReturnedResolution())));
  }

  @Test
  void checkInByBarcodeWithServicePointAndNullClaimedReturnedResolutionTest() {
    circulationService.checkInByBarcode(createTransactionEntity(), randomUUID().toString(), null);
    verify(circulationClient).checkInByBarcode(argThat(req -> req.getClaimedReturnedResolution() == null));
  }

  @Test
  void shouldUpdateRequestApiWithIsDcbRerequestCancellationTrue() {
    CirculationRequest fetchedRequest = createCirculationRequest();
    fetchedRequest.setIsDcbReRequestCancellation(null);
    CirculationRequest requestToBeCancelled = createCirculationRequest();
    requestToBeCancelled.setIsDcbReRequestCancellation(true);
    when(circulationRequestService.getCancellationRequestIfOpenOrNull(anyString())).thenReturn(fetchedRequest);
    circulationService.cancelRequest(createTransactionEntity(), true);
    verify(circulationClient).updateRequest(requestToBeCancelled.getId(), requestToBeCancelled);
  }

  @Test
  void cancelRequestTest() {
    when(circulationRequestService.getCancellationRequestIfOpenOrNull(anyString()))
      .thenReturn(createCirculationRequest());
    circulationService.cancelRequest(createTransactionEntity(), false);
    verify(circulationClient).updateRequest(anyString(), any());
  }

  @Test
  void shouldThrowExceptionWhenRequestIsNotUpdated() {
    TransactionEntity transactionEntity = createTransactionEntity();
    when(circulationRequestService.getCancellationRequestIfOpenOrNull(anyString()))
      .thenReturn(createCirculationRequest());
    when(circulationClient.updateRequest(anyString(), any()))
      .thenThrow(HttpClientErrorException.BadRequest.class);
    assertThrows(CirculationRequestException.class, () ->
      circulationService.cancelRequest(transactionEntity, false));
  }

  @Test
  void checkInByBarcodeShouldPrioritizeMethodParameterServicePointIdTest() {
    // TestMate-8336b6a9423d10f6280b51c55a4066cb
    var transactionEntity = createTransactionEntity();
    transactionEntity.setServicePointId("ENTITY_SERVICE_POINT");
    transactionEntity.setItemBarcode("ITEM-123");
    String explicitServicePointId = "EXPLICIT_SERVICE_POINT";

    circulationService.checkInByBarcode(transactionEntity, explicitServicePointId, null);

    verify(circulationClient).checkInByBarcode(argThat(req ->
      explicitServicePointId.equals(req.getServicePointId())
        && "ITEM-123".equals(req.getItemBarcode())
    ));
  }

  @Test
  void checkInByBarcode_positive_servicePointIsNull() {
    // TestMate-72b46a8ea31a64a588009299c2871c1b
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemBarcode("99999");
    transactionEntity.setServicePointId(null);

    circulationService.checkInByBarcode(transactionEntity);

    verify(circulationClient).checkInByBarcode(argThat(request ->
      "99999".equals(request.getItemBarcode()) && request.getServicePointId() == null
    ));
  }

  @Test
  void cancelRequest_positive_noOpenRequestFound() {
    // TestMate-b7855e7929105a63d4324d9664993f01
    var transactionEntity = createTransactionEntity();
    when(circulationRequestService.getCancellationRequestIfOpenOrNull(anyString())).thenReturn(null);
    circulationService.cancelRequest(transactionEntity, false);
    verify(circulationClient, never()).updateRequest(anyString(), any());
  }

    @Test
  void checkOutByBarcodeTest() {
    // TestMate-cbede54e61b377c837c70259d4001b54
    // Given
    TransactionEntity transactionEntity = createTransactionEntity();
    transactionEntity.setId("TRANSACTION-001");
    transactionEntity.setItemBarcode("ITEM-123");
    transactionEntity.setPatronBarcode("PATRON-456");
    transactionEntity.setServicePointId("SP-789");
    // When
    circulationService.checkOutByBarcode(transactionEntity);
    // Then
    verify(circulationClient).checkOutByBarcode(argThat(request ->
      "ITEM-123".equals(request.getItemBarcode()) &&
        "PATRON-456".equals(request.getUserBarcode()) &&
        "SP-789".equals(request.getServicePointId())
    ));
  }

    @Test
  void checkOutByBarcode_positive_servicePointIsNull() {
    // TestMate-516fb67d92eaf9028055dcddb3cf3839
    // Given
    TransactionEntity transactionEntity = createTransactionEntity();
    transactionEntity.setItemBarcode("ITEM-999");
    transactionEntity.setPatronBarcode("PATRON-888");
    transactionEntity.setServicePointId(null);
    // When
    circulationService.checkOutByBarcode(transactionEntity);
    // Then
    verify(circulationClient).checkOutByBarcode(argThat(request ->
      "ITEM-999".equals(request.getItemBarcode()) &&
        "PATRON-888".equals(request.getUserBarcode()) &&
        request.getServicePointId() == null
    ));
  }
}
