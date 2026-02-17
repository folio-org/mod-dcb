package org.folio.dcb.integration.invstorage;

import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.InventoryItem;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("item-storage")
public interface InventoryItemStorageClient {

  @GetExchange("/items")
  ResultList<InventoryItem> findByQuery(@RequestParam("query") String query);
}
