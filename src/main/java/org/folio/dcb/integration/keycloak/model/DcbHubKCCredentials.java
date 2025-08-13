package org.folio.dcb.integration.keycloak.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DcbHubKCCredentials {

  @JsonProperty("client_id")
  private String clientId;

  @JsonProperty("client_secret")
  private String clientSecret;

  @JsonProperty("username")
  private String username;

  @JsonProperty("password")
  private String password;

  @JsonProperty("keycloak_url")
  private String keycloakUrl;

  @JsonProperty("authentication_type")
  private String authenticationType;
}
