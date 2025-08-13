package org.folio.dcb.integration.keycloak;

import org.folio.dcb.integration.keycloak.model.DcbHubKCCredentials;
import org.folio.dcb.integration.keycloak.model.TokenContainer;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class DcbHubKCTokenService {

  private final KeyCloakLoginService keyCloakLoginService;
  private static final String BEARER = "Bearer ";

  /**
   * Performs login operation using Keycloak OpenID Token Endpoint.
   *
   * @return {@link TokenContainer} object with access and refresh tokens populated with additional information
   */
  public TokenContainer getToken(DcbHubKCCredentials dcbHubKCCredentials) {
    return keyCloakLoginService.login(dcbHubKCCredentials);
  }

  public String getBearerAccessToken(DcbHubKCCredentials dcbHubKCCredentials) {
    return BEARER + getToken(dcbHubKCCredentials).getAccessToken().getJwt();
  }
}
