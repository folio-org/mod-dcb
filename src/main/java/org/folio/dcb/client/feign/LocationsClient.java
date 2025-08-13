package org.folio.dcb.client.feign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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

import java.util.List;

@FeignClient(name = "locations", configuration = FeignClientConfiguration.class)
public interface LocationsClient {

  @PostMapping
  LocationDTO createLocation(@RequestBody LocationDTO locationDTO);
  @GetMapping("?query=name=={name}")
  ResultList<LocationDTO> queryLocationsByName(@PathVariable("name") String name);

  @GetMapping
  ResultList<LocationDTO> findLocationByQuery(
    @RequestParam("query") String query,
    @RequestParam("includeShadowLocations") Boolean includeShadowLocations,
    @RequestParam("limit") int limit,
    @RequestParam("offset") int offset);


  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  class LocationDTO {
    private String id;
    private String name;
    private String code;
    private String institutionId;
    private String campusId;
    private String libraryId;
    private String primaryServicePoint;
    private List<String> servicePointIds;
    @Builder.Default
    @JsonProperty("isShadow")
    private boolean isShadow = false;
  }

}
