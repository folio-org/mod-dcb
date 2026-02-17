package org.folio.dcb.integration.invstorage;

import org.folio.dcb.domain.ResultList;
import org.folio.dcb.integration.invstorage.model.InventoryHolding;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("holdings-storage")
public interface HoldingsStorageClient {

  @GetExchange("/holdings")
  ResultList<InventoryHolding> findByQuery(@RequestParam("query") String query);

  @GetExchange("/holdings/{holdingId}")
  InventoryHolding findHolding(@PathVariable String holdingId);

  @PostExchange("/holdings")
  InventoryHolding createHolding(@RequestBody InventoryHolding holding);
}
