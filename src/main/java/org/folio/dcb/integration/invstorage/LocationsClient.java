package org.folio.dcb.integration.invstorage;

import org.folio.dcb.domain.ResultList;
import org.folio.dcb.integration.invstorage.model.Location;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("locations")
public interface LocationsClient {

  @PostExchange
  Location createLocation(@RequestBody Location location);

  @GetExchange
  ResultList<Location> findByQuery(@RequestParam("query") String query);

  @GetExchange
  ResultList<Location> findByQuery(
    @RequestParam("query") String query,
    @RequestParam("includeShadowLocations") Boolean includeShadowLocations);

  @GetExchange
  ResultList<Location> findLocationByQuery(
    @RequestParam("query") String query,
    @RequestParam("includeShadowLocations") Boolean includeShadowLocations,
    @RequestParam("limit") int limit,
    @RequestParam("offset") int offset);
}
