package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.MaterialTypeCollection;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "material-types", configuration = FeignClientConfiguration.class)
public interface MaterialTypeClient {
  @GetMapping
  MaterialTypeCollection fetchMaterialTypeByQuery(@RequestParam("query") String query);
}
