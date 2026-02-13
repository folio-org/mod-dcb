package org.folio.dcb.integration.inventory;

import org.folio.dcb.domain.ResultList;
import org.folio.dcb.integration.inventory.model.InventoryInstance;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("inventory")
public interface InstanceClient {

  @GetExchange(value = "/instances")
  ResultList<InventoryInstance> findByQuery(@RequestParam("query") String query);

  @PostExchange("/instances")
  void createInstance(@RequestBody InventoryInstance instance);
}
