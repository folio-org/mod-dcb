package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.CirculationItemCollection;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "circulation-item", configuration = FeignClientConfiguration.class)
public interface CirculationItemClient {
  @PostMapping("/{circulationItemId}")
  CirculationItem createCirculationItem(@PathVariable("circulationItemId") String circulationItemId, @RequestBody CirculationItem circulationRequest);

  @GetMapping("/{circulationItemId}")
  CirculationItem retrieveCirculationItemById(@PathVariable("circulationItemId") String circulationItemId);

  @GetMapping
  CirculationItemCollection fetchItemByCqlQuery(@RequestParam("query") String query);}
