package org.folio.dcb.integration.circitem;

import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.CirculationItemCollection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange("circulation-item")
public interface CirculationItemClient {

  @PostExchange("/{circulationItemId}")
  CirculationItem createCirculationItem(
    @PathVariable String circulationItemId,
    @RequestBody CirculationItem circulationRequest);

  @GetExchange("/{circulationItemId}")
  CirculationItem retrieveCirculationItemById(@PathVariable String circulationItemId);

  @GetExchange
  CirculationItemCollection fetchItemByCqlQuery(@RequestParam("query") String query);

  @PutExchange("/{circulationItemId}")
  CirculationItem updateCirculationItem(
    @PathVariable String circulationItemId,
    @RequestBody CirculationItem circulationItem);
}
