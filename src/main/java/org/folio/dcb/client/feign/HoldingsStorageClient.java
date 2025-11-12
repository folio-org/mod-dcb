package org.folio.dcb.client.feign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.dcb.domain.ResultList;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "holdings-storage", configuration = FeignClientConfiguration.class)
public interface HoldingsStorageClient {

  @GetMapping("/holdings")
  ResultList<Holding> findByQuery(@RequestParam("query") String query);

  @GetMapping("/holdings/{holdingId}")
  Holding findHolding(@PathVariable("holdingId") String holdingId);

  @PostMapping("/holdings")
  Holding createHolding(@RequestBody Holding holding);

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
