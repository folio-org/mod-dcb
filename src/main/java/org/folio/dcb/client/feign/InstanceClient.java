package org.folio.dcb.client.feign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory", configuration = FeignClientConfiguration.class)
public interface InstanceClient {

  @GetMapping(value = "instances/{instanceId}")
  JsonNode getInstanceById(@PathVariable String instanceId);

  @GetMapping(value = "/instances")
  ResultList<InventoryInstanceDTO> findByQuery(@RequestParam("query") String query);

  @PostMapping("/instances")
  void createInstance(@RequestBody InventoryInstanceDTO instance);

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  class InventoryInstanceDTO {
    private String id;
    private String instanceTypeId;
    private String title;
    private String source;
  }

}
