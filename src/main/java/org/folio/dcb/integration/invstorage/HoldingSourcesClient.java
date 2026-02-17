package org.folio.dcb.integration.invstorage;

import org.folio.dcb.domain.ResultList;
import org.folio.dcb.integration.invstorage.model.HoldingSource;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("holdings-sources")
public interface HoldingSourcesClient {

  @GetExchange
  ResultList<HoldingSource> findByQuery(@RequestParam("query") String query);

  @PostExchange
  HoldingSource createHoldingsRecordSource(@RequestBody HoldingSource holdingSource);

}
