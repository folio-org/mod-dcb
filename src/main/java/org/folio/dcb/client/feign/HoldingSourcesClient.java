package org.folio.dcb.client.feign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "holdings-sources", configuration = FeignClientConfiguration.class)
public interface HoldingSourcesClient {
  @GetMapping("?query=name=={sourceName}")
  HoldingSource querySourceByName(@PathVariable("sourceName") String sourceName);

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  class HoldingSource {
    private String id;
  }
}
