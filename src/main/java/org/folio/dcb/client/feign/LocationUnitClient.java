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

@FeignClient(name = "location-units", configuration = FeignClientConfiguration.class)
public interface LocationUnitClient {

  @PostMapping("/institutions")
  LocationUnit createInstitution(@RequestBody LocationUnit locationUnit);
  @GetMapping("/institutions/{institutionId}")
  LocationUnit getInstitutionById(@PathVariable("institutionId") String institutionId);
  @GetMapping("/institutions?query=(name=={name} AND code=={code})")
  ResultList<LocationUnit> queryInstitutionByNameAndCode(@PathVariable("name") String name, @PathVariable("code") String code);

  @PostMapping("/campuses")
  LocationUnit createCampus(@RequestBody LocationUnit locationUnit);
  @GetMapping("/campuses?query=(name=={name})")
  ResultList<LocationUnit> getCampusByName(@PathVariable("name") String name);
  @GetMapping("/campuses?query=(name=={name} AND code=={code})")
  ResultList<LocationUnit> queryCampusByNameAndCode(@PathVariable("name") String name, @PathVariable("code") String code);

  @PostMapping("/libraries")
  LocationUnit createLibrary(@RequestBody LocationUnit locationUnit);
  @GetMapping("/libraries?query=(name=={name})")
  ResultList<LocationUnit> getLibraryByName(@PathVariable("name") String name);
  @GetMapping("/libraries?query=(name=={name} AND code=={code})")
  ResultList<LocationUnit> queryLibraryByNameAndCode(@PathVariable("name") String name, @PathVariable("code") String code);

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
  }
}
