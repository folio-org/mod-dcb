package org.folio.dcb.integration.keycloak.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Builder
public class KeycloakAuthentication {

  /**
   * JWT Access Token.
   */
  @JsonProperty("access_token")
  private String accessToken;

  /**
   * JWT Refresh Token.
   */
  @JsonProperty("refresh_token")
  private String refreshToken;


  /**
   * Access token expiration age.
   */
  @JsonProperty("expires_in")
  private Long expiresIn;

  /**
   * Refresh token expiration age.
   */
  @JsonProperty("refresh_expires_in")
  private Long refreshExpiresIn;

  @JsonProperty("token_type")
  private String tokenType;

  @JsonProperty("session_state")
  private String sessionState;

  @JsonProperty("scope")
  private String scope;
}
