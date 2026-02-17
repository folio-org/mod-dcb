package org.folio.dcb.integration.invstorage;

import org.folio.dcb.domain.ResultList;
import org.folio.dcb.integration.invstorage.model.LocationUnit;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("location-units")
public interface LocationUnitClient {

  @PostExchange("/institutions")
  LocationUnit createInstitution(@RequestBody LocationUnit locationUnit);

  @GetExchange("/institutions")
  ResultList<LocationUnit> findInstitutionsByQuery(@RequestParam("query") String query);

  @GetExchange("/institutions")
  ResultList<LocationUnit> findInstitutionsByQuery(
    @RequestParam("query") String query,
    @RequestParam("includeShadow") Boolean includeShadow,
    @RequestParam("limit") int limit,
    @RequestParam("offset") int offset);

  @PostExchange("/campuses")
  LocationUnit createCampus(@RequestBody LocationUnit locationUnit);

  @GetExchange("/campuses")
  ResultList<LocationUnit> findCampusesByQuery(@RequestParam("query") String query);

  @GetExchange("/libraries")
  ResultList<LocationUnit> findLibrariesByQuery(@RequestParam("query") String query);

  @GetExchange("/campuses")
  ResultList<LocationUnit> findCampusesByQuery(
    @RequestParam("query") String query,
    @RequestParam("includeShadow") Boolean includeShadow,
    @RequestParam("limit") int limit,
    @RequestParam("offset") int offset);

  @PostExchange("/libraries")
  LocationUnit createLibrary(@RequestBody LocationUnit locationUnit);

  @GetExchange("/libraries")
  ResultList<LocationUnit> findLibrariesByQuery(
    @RequestParam("query") String query,
    @RequestParam("includeShadow") Boolean includeShadow,
    @RequestParam("limit") int limit,
    @RequestParam("offset") int offset);
}
