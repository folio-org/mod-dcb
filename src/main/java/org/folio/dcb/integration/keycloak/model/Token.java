package org.folio.dcb.integration.keycloak.model;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Token {

  /**
   * JWT.
   */
  private String jwt;

  /**
   * Token expiration age is seconds.
   */
  private Long expiresIn;

  /**
   * Token expiration date.
   */
  private Instant expirationDate;
}
