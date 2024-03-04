package org.folio.dcb.service;

import org.folio.dcb.domain.dto.CirculationRequest;

public interface CirculationRequestService {
  CirculationRequest getCancellationRequestIfOpenOrNull(String requestId);
  CirculationRequest fetchRequestById(String requestId);
}
