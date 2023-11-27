package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.InventoryItem;
import org.folio.spring.config.FeignClientConfiguration;
import org.folio.spring.model.ResultList;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "item-storage", configuration = FeignClientConfiguration.class)
public interface InventoryItemStorageClient {
  @GetMapping("/items/{itemId}")
  InventoryItem findItem(@PathVariable("itemId") String itemId);

  @GetMapping("/items")
  ResultList<InventoryItem> fetchItemByBarcode(@RequestParam("query") String query);
}
