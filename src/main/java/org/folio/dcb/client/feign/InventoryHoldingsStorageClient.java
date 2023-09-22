package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.InventoryHolding;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "holdings-storage", configuration = FeignClientConfiguration.class)
public interface InventoryHoldingsStorageClient {
  @GetMapping("/holdings/{holdingId}")
  InventoryHolding findHolding(@PathVariable("holdingId") String holdingId);
}
