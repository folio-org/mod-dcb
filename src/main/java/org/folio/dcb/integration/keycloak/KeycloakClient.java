package org.folio.dcb.integration.keycloak;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

import java.net.URI;
import java.util.Map;

import org.folio.dcb.integration.keycloak.config.DcbHubKeycloakConfiguration;
import org.folio.dcb.integration.keycloak.model.KeycloakAuthentication;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import feign.Headers;

@FeignClient(
  name = "keycloak-client",
  configuration = DcbHubKeycloakConfiguration.class
)
public interface KeycloakClient {

  @PostMapping(consumes = APPLICATION_FORM_URLENCODED_VALUE)
  @Headers("Content-Type: application/x-www-form-urlencoded")
  KeycloakAuthentication callTokenEndpoint(URI uri, @RequestBody Map<String, ?> formData);
}
