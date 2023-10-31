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


import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(value = "instance-types", configuration = FeignClientConfiguration.class)
public interface InstanceTypeClient {

  @GetMapping(value = "?query=(name=={name})", produces = APPLICATION_JSON_VALUE)
  ResultList<InstanceType> queryInstanceTypeByName(@PathVariable("name") String name);
  @PostMapping
  InstanceType createInstanceType(@RequestBody InstanceType instanceType);

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  class InstanceType {
    private String id;
    private String name;
    private String code;
    private String source;
  }
}
