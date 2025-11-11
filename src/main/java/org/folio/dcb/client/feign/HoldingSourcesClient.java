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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "holdings-sources", configuration = FeignClientConfiguration.class)
public interface HoldingSourcesClient {

  @GetMapping
  ResultList<HoldingSource> findByQuery(@RequestParam("query") String query);

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
