package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.User;
import org.folio.dcb.domain.dto.UserCollection;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "users", contextId = "UsersClientDcb", configuration = FeignClientConfiguration.class)
public interface UsersClient {
  @PostMapping
  User createUser(@RequestBody User user);

  @GetMapping
  UserCollection fetchUserByBarcodeAndId(@RequestParam("query") String query);

  @PutMapping("/{userId}")
  User updateUser(@PathVariable("userId") String userId, @RequestBody User user);
}
