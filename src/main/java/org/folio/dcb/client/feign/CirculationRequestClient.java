package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "request-storage", configuration = FeignClientConfiguration.class)
public interface CirculationRequestClient {
  @GetMapping("/requests/{circulationItemId}")
  CirculationRequest fetchRequestById(@PathVariable("circulationItemId") String circulationItemId);

}
