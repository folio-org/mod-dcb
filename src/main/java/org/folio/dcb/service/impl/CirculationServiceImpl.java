package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.domain.dto.CheckInRequest;
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
    circulationClient.checkInByBarcode(createCheckInRequest(dcbTransaction.getItemBarcode(), "3a40852d-49fd-4df2-a1f9-6e2641a6e91f"));
  }

  private CheckInRequest createCheckInRequest(String itemBarcode, String servicePointId){
    return CheckInRequest.builder()
      .itemBarcode(itemBarcode)
      .servicePointId(servicePointId)
      .checkInDate(OffsetDateTime.now().toString()).build();
  }
}
