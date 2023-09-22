package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "circulation", configuration = FeignClientConfiguration.class)
public interface RequestClient {
  @PostMapping("/requests")
  CirculationRequest createRequest(@RequestBody CirculationRequest circulationRequest);
}
