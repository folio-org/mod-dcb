package org.folio.dcb.integration.keycloak;

import org.folio.dcb.integration.keycloak.model.DcbHubKCCredentials;
import org.folio.dcb.integration.keycloak.model.KeycloakAuthentication;
import org.folio.dcb.integration.keycloak.model.Token;
import org.folio.dcb.integration.keycloak.model.TokenContainer;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class KeyCloakLoginService {

  private final KeycloakService keycloakService;
  private final JwtTokenParser tokenParser;

  /**
   * Performs login operation using Keycloak OpenID Token Endpoint.
   *
   * @param dcbHubKCCredentials  - dcb hub keycloak credentials
   * @return {@link TokenContainer} object with access and refresh tokens populated with additional information
   */
  public TokenContainer login(DcbHubKCCredentials dcbHubKCCredentials) {
    var keycloakAuthentication = keycloakService.getUserToken(dcbHubKCCredentials);
    return buildTokenContainer(keycloakAuthentication);
  }

  private TokenContainer buildTokenContainer(KeycloakAuthentication keycloakAuthentication) {
    var accessToken = Token.builder().jwt(keycloakAuthentication.getAccessToken())
      .expirationDate(tokenParser.parseExpirationDate(keycloakAuthentication.getAccessToken()))
      .expiresIn(keycloakAuthentication.getExpiresIn())
      .build();
    var refreshToken = Token.builder().jwt(keycloakAuthentication.getRefreshToken())
      .expirationDate(tokenParser.parseExpirationDate(keycloakAuthentication.getRefreshToken()))
      .expiresIn(keycloakAuthentication.getRefreshExpiresIn())
      .build();
    return TokenContainer.builder().accessToken(accessToken).refreshToken(refreshToken).build();
  }


}
