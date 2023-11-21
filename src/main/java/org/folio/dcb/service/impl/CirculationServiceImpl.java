package org.folio.dcb.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.domain.dto.CheckInRequest;
import org.folio.dcb.domain.dto.CheckOutRequest;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.exception.CirculationRequestException;
import org.folio.dcb.service.CirculationService;
import org.folio.dcb.service.CirculationRequestService;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationServiceImpl implements CirculationService {

  private final CirculationClient circulationClient;
  private final CirculationRequestService circulationStorageService;

  @Override
  public void checkInByBarcode(TransactionEntity dcbTransaction) {
    log.debug("checkInByBarcode:: automate item checkIn using  dcbTransaction {} ", dcbTransaction);
    circulationClient.checkInByBarcode(createCheckInRequest(dcbTransaction.getItemBarcode(), dcbTransaction.getServicePointId()));
  }

  @Override
  public void checkInByBarcode(TransactionEntity dcbTransaction, String servicePointId) {
    log.debug("checkInByBarcode:: automate item checkIn using  dcbTransaction {} ", dcbTransaction);
    circulationClient.checkInByBarcode(createCheckInRequest(dcbTransaction.getItemBarcode(), servicePointId));
  }

  @Override
  public void checkOutByBarcode(TransactionEntity dcbTransaction) {
    log.debug("checkOutByBarcode:: automate item checkOut using  dcbTransaction {} ", dcbTransaction);
    circulationClient.checkOutByBarcode(createCheckOutRequest(dcbTransaction.getItemBarcode(), dcbTransaction.getPatronBarcode(), dcbTransaction.getServicePointId()));
  }

  @Override
  public void cancelRequest(TransactionEntity dcbTransaction) {
    log.debug("cancelRequest:: cancelling request using request id {} ", dcbTransaction.getRequestId());
    CirculationRequest request = circulationStorageService.getCancellationRequestIfOpenOrNull(dcbTransaction.getRequestId().toString());
    if (request != null){
      try {
        circulationClient.cancelRequest(request.getId(), request);
      } catch (FeignException e) {
        log.warn("cancelRequest:: error cancelling request using request id {} ", dcbTransaction.getRequestId(), e);
        throw new CirculationRequestException(String.format("Error cancelling request using request id %s", dcbTransaction.getRequestId()));
      }
    }
  }

  private CheckInRequest createCheckInRequest(String itemBarcode, String servicePointId){
    return CheckInRequest.builder()
      .itemBarcode(itemBarcode)
      .servicePointId(servicePointId)
      .checkInDate(OffsetDateTime.now().toString())
      .build();
  }

  private CheckOutRequest createCheckOutRequest(String itemBarcode, String userBarcode, String servicePointId){
    return CheckOutRequest.builder()
      .itemBarcode(itemBarcode)
      .userBarcode(userBarcode)
      .servicePointId(servicePointId)
      .build();
  }
}
