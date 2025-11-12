package org.folio.dcb.client.feign;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.folio.spring.config.FeignClientConfiguration;
import org.folio.spring.model.ResultList;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "location-units", configuration = FeignClientConfiguration.class)
public interface LocationUnitClient {

  @PostMapping("/institutions")
  LocationUnit createInstitution(@RequestBody LocationUnit locationUnit);

  @GetMapping("/institutions")
  ResultList<LocationUnit> findInstitutionsByQuery(@RequestParam("query") String query);

  @GetMapping("/institutions")
  ResultList<LocationUnit> findInstitutionsByQuery(
    @RequestParam("query") String query,
    @RequestParam("includeShadow") Boolean includeShadow,
    @RequestParam("limit") int limit,
    @RequestParam("offset") int offset);

  @PostMapping("/campuses")
  LocationUnit createCampus(@RequestBody LocationUnit locationUnit);

  @GetMapping("/campuses")
  ResultList<LocationUnit> findCampusesByQuery(@RequestParam("query") String query);

  @GetMapping("/libraries")
  ResultList<LocationUnit> findLibrariesByQuery(@RequestParam("query") String query);

  @GetMapping("/campuses")
  ResultList<LocationUnit> findCampusesByQuery(
    @RequestParam("query") String query,
    @RequestParam("includeShadow") Boolean includeShadow,
    @RequestParam("limit") int limit,
    @RequestParam("offset") int offset);

  @PostMapping("/libraries")
  LocationUnit createLibrary(@RequestBody LocationUnit locationUnit);

  @GetMapping("/libraries")
  ResultList<LocationUnit> findLibrariesByQuery(
    @RequestParam("query") String query,
    @RequestParam("includeShadow") Boolean includeShadow,
    @RequestParam("limit") int limit,
    @RequestParam("offset") int offset);

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  class LocationUnit {
    private String id;
    private String name;
    private String code;
    private String institutionId;
    private String campusId;
    private String libraryId;

    @Builder.Default
    @JsonProperty("isShadow")
    @Getter(onMethod_ = @JsonIgnore)
    private boolean isShadow = false;
  }
}
