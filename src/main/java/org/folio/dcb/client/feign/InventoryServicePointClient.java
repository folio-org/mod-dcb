package org.folio.dcb.client.feign;

import org.folio.dcb.client.feign.config.DcbClientConfiguration;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "service-points", configuration = DcbClientConfiguration.class)
public interface InventoryServicePointClient {

  @PostMapping
  ServicePointRequest createServicePoint(@RequestBody ServicePointRequest pickupServicePoint);

}
