package org.folio.dcb.integration.circstorage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.dcb.domain.ResultList;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("cancellation-reason-storage")
public interface CancellationReasonClient {

  @GetExchange("/cancellation-reasons")
  ResultList<CancellationReason> findByQuery(@RequestParam("query") String query);

  @PostExchange("/cancellation-reasons")
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
