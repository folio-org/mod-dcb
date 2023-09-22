package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.UserGroupCollection;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "groups", configuration = FeignClientConfiguration.class)
public interface GroupClient {
  @GetMapping
  UserGroupCollection fetchGroupByName(@RequestParam("query") String query);
}
