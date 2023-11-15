package org.folio.dcb.service;

import org.folio.dcb.domain.dto.CirculationRequest;

public interface CirculationRequestService {
  CirculationRequest fetchRequestById(String requestId);
  CirculationRequest getCancellationRequestIfOpenOrNull(String requestId);
}
