package org.folio.dcb.integration.circstorage;

import org.folio.dcb.domain.dto.CirculationRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("request-storage")
public interface CirculationRequestClient {

  @GetExchange("/requests/{requestId}")
  CirculationRequest fetchRequestById(@PathVariable String requestId);
}
