package org.folio.dcb.integration.keycloak.model;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenContainer {

  private Token accessToken;
  private Token refreshToken;

  public Instant getAccessTokenExpirationDate() {
    if (accessToken == null) {
      return null;
    }
    return accessToken.getExpirationDate();
  }

  public Instant getRefreshTokenExpirationDate() {
    if (refreshToken == null) {
      return null;
    }
    return refreshToken.getExpirationDate();
  }

  /**
   * Creates a {@link TokenContainer} with null access and refresh tokens.
   *
   * @return empty {@link TokenContainer} object
   */
  public static TokenContainer empty() {
    return new TokenContainer(null, null);
  }
}
