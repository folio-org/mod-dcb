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

@FeignClient(name = "cancellation-reason-storage", configuration = FeignClientConfiguration.class)
public interface CancellationReasonClient {

  @GetMapping("/cancellation-reasons")
  ResultList<CancellationReason> findByQuery(@RequestParam("query") String query);

  @PostMapping("/cancellation-reasons")
  CancellationReason createCancellationReason(@RequestBody CancellationReason cancellationReason);

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  class CancellationReason {
    private String id;
    private String name;
    private String description;
  }
}
