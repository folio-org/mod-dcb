package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.domain.dto.CheckInRequest;
import org.folio.dcb.domain.dto.CheckOutRequest;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.service.CirculationService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationServiceImpl implements CirculationService {
  private final CirculationClient circulationClient;

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
