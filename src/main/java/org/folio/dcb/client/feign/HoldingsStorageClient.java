package org.folio.dcb.client.feign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "holdings-storage", configuration = FeignClientConfiguration.class)
public interface HoldingsStorageClient {

  @GetMapping("/holdings/{holdingId}")
  Holding findHolding(@PathVariable("holdingId") String holdingId);

  @PostMapping("/holdings")
  void createHolding(@RequestBody Holding holding);

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  class Holding {
    private String id;
    private String instanceId;
    private String permanentLocationId;
    private String sourceId;
  }
}
