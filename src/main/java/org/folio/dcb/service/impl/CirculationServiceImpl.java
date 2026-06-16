package org.folio.dcb.service.impl;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.CheckInRequest;
import org.folio.dcb.domain.dto.CheckOutRequest;
import org.folio.dcb.domain.dto.ClaimedReturnedResolution;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.exception.CirculationRequestException;
import org.folio.dcb.integration.circulation.CirculationClient;
import org.folio.dcb.service.CirculationRequestService;
import org.folio.dcb.service.CirculationService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationServiceImpl implements CirculationService {

  private final CirculationClient circulationClient;
  private final CirculationRequestService circulationStorageService;

  @Override
  public void checkInByBarcode(TransactionEntity dcbTransaction) {
    log.debug("checkInByBarcode:: checking in item for transaction {}.", dcbTransaction.getId());
    circulationClient.checkInByBarcode(
      createCheckInRequest(dcbTransaction.getItemBarcode(), dcbTransaction.getServicePointId()));
  }

  @Override
  public void checkInByBarcode(TransactionEntity dcbTransaction, String servicePointId) {
    log.debug("checkInByBarcode:: checking in item for transaction {}.", dcbTransaction.getId());
    circulationClient.checkInByBarcode(createCheckInRequest(dcbTransaction.getItemBarcode(), servicePointId));
  }

  @Override
  public void checkInByBarcode(TransactionEntity dcbTransaction, String servicePointId,
    ClaimedReturnedResolution claimedReturnedResolution) {

    log.info("checkInByBarcode:: checking in item for transaction {} with claimedReturnedResolution '{}'.",
      dcbTransaction.getId(), claimedReturnedResolution);
    circulationClient.checkInByBarcode(createCheckInRequest(dcbTransaction.getItemBarcode(), servicePointId,
      claimedReturnedResolution));
  }

  @Override
  public void checkOutByBarcode(TransactionEntity dcbTransaction) {
    log.debug("checkOutByBarcode:: checking out item for transaction {}.", dcbTransaction.getId());
    circulationClient.checkOutByBarcode(createCheckOutRequest(
      dcbTransaction.getItemBarcode(), dcbTransaction.getPatronBarcode(), dcbTransaction.getServicePointId()));
  }

  @Override
  public void cancelRequest(TransactionEntity dcbTransaction, boolean isItemUnavailableCancellation) {
    var requestId = dcbTransaction.getRequestId();
    log.debug("cancelRequest:: cancelling request using request id {} ", requestId);
    var request = circulationStorageService.getCancellationRequestIfOpenOrNull(requestId.toString());
    if (request != null) {
      try {
        if (isItemUnavailableCancellation) {
          request.setIsDcbReRequestCancellation(true);
        }
        circulationClient.updateRequest(request.getId(), request);
      } catch (HttpClientErrorException e) {
        log.warn("cancelRequest:: error cancelling request using request id {} ", requestId, e);
        throw new CirculationRequestException(String.format("Error cancelling request using request id %s", requestId));
      }
    }
  }

  private CheckInRequest createCheckInRequest(String itemBarcode, String servicePointId) {
    return createCheckInRequest(itemBarcode, servicePointId, null);
  }

  private CheckInRequest createCheckInRequest(String itemBarcode, String servicePointId,
    ClaimedReturnedResolution claimedReturnedResolution) {

    return CheckInRequest.builder()
      .itemBarcode(itemBarcode)
      .servicePointId(servicePointId)
      .checkInDate(OffsetDateTime.now().toString())
      .claimedReturnedResolution(claimedReturnedResolution)
      .build();
  }

  private CheckOutRequest createCheckOutRequest(String itemBarcode, String userBarcode, String servicePointId) {
    return CheckOutRequest.builder()
      .itemBarcode(itemBarcode)
      .userBarcode(userBarcode)
      .servicePointId(servicePointId)
      .build();
  }
}
