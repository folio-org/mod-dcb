package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.CirculationItemRequest;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "circulation-item", configuration = FeignClientConfiguration.class)
public interface CirculationItemClient {
  @PostMapping("/{circulationItemId}")
  CirculationItemRequest createCirculationItem(@PathVariable("circulationItemId") String circulationItemId, @RequestBody CirculationItemRequest circulationRequest);

  @GetMapping("/{circulationItemId}")
  CirculationItemRequest retrieveCirculationItemById(@PathVariable("circulationItemId") String circulationItemId);

  @PutMapping("/{circulationItemId}")
  CirculationItemRequest updateCirculationItem(@PathVariable("circulationItemId") String circulationItemId, @RequestBody CirculationItemRequest circulationRequest);
}
