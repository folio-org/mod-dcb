package org.folio.dcb.client.feign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.spring.config.FeignClientConfiguration;
import org.folio.spring.model.ResultList;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "holdings-sources", configuration = FeignClientConfiguration.class)
public interface HoldingSourcesClient {
  @GetMapping("?query=name=={sourceName}")
  ResultList<HoldingSource> querySourceByName(@PathVariable("sourceName") String sourceName);
  @PostMapping
  HoldingSource createHoldingsRecordSource(@RequestBody HoldingSourcesClient.HoldingSource holdingSource);

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  class HoldingSource {
    private String id;
    private String name;
    private String source;
  }
}
