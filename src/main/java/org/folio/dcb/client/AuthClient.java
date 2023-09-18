package org.folio.dcb.client;

import org.folio.dcb.domain.dto.SystemUserParameters;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("authn")
public interface AuthClient {

  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<String> getApiKey(@RequestBody SystemUserParameters authObject);

  @PostMapping(value = "/credentials", consumes = MediaType.APPLICATION_JSON_VALUE)
  void saveCredentials(@RequestBody SystemUserParameters systemUserParameters);
}
