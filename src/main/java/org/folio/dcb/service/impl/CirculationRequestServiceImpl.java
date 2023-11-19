package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.CirculationRequestClient;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.service.CirculationRequestService;
import org.folio.dcb.utils.DCBConstants;
import org.folio.dcb.utils.RequestStatus;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationRequestServiceImpl implements CirculationRequestService {

  private final CirculationRequestClient circulationStorageClient;
  private final FolioExecutionContext folioExecutionContext;

  @Override
  public CirculationRequest fetchRequestById(String requestId) {
    log.info("fetchRequestById:: fetching request for id {} ", requestId);
    return circulationStorageClient.fetchRequestById(requestId);
  }

  @Override
  public CirculationRequest getCancellationRequestIfOpenOrNull(String requestId){
    CirculationRequest request = fetchRequestById(requestId);
    if(request != null && RequestStatus.isRequestOpen(RequestStatus.from(request.getStatus()))){
      request.setStatus(RequestStatus.CLOSED_CANCELLED.getValue());
      request.setCancelledDate(String.valueOf(new Date()));
      request.setCancellationAdditionalInformation("Request cancelled by DCB");
      request.setCancelledByUserId(folioExecutionContext.getUserId());
      request.setCancellationReasonId(UUID.fromString(DCBConstants.CANCELLATION_REASON_ID));
      return request;
    }
    return null;
  }
}