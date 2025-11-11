package org.folio.dcb.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.CirculationRequestClient;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.service.CirculationRequestService;
import org.folio.dcb.service.entities.DcbEntityServiceFacade;
import org.folio.dcb.utils.RequestStatus;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationRequestServiceImpl implements CirculationRequestService {

  private final CirculationRequestClient circulationRequestClient;
  private final FolioExecutionContext folioExecutionContext;
  private final DcbEntityServiceFacade dcbEntityServiceFacade;

  public CirculationRequest fetchRequestById(String requestId) {
    log.info("fetchRequestById:: fetching request for id {} ", requestId);
    try {
      return circulationRequestClient.fetchRequestById(requestId);
    } catch (FeignException.NotFound e) {
      log.warn("Circulation request not found by id={}", requestId);
      return null;
    }
  }

  @Override
  public CirculationRequest getCancellationRequestIfOpenOrNull(String requestId){
    CirculationRequest request = fetchRequestById(requestId);
    if(request != null && RequestStatus.isRequestOpen(RequestStatus.from(request.getStatus()))){
      var cancellationReason = dcbEntityServiceFacade.findOrCreateCancellationReason();
      request.setStatus(RequestStatus.CLOSED_CANCELLED.getValue());
      request.setCancelledDate(OffsetDateTime.now().toString());
      request.setCancellationAdditionalInformation("Request cancelled by DCB");
      request.setCancelledByUserId(folioExecutionContext.getUserId());
      request.setCancellationReasonId(UUID.fromString(cancellationReason.getId()));
      return request;
    }
    return null;
  }
}
