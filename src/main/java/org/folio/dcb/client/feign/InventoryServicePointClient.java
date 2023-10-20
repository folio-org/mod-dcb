package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "service-points", configuration = FeignClientConfiguration.class)
public interface InventoryServicePointClient {

  @PostMapping
  ServicePointRequest createServicePoint(@RequestBody ServicePointRequest pickupServicePoint);

}
