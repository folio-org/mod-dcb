package org.folio.dcb.integration.keycloak;

import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

import java.net.URI;
import java.util.Map;

import org.folio.dcb.exception.ServiceException;
import org.folio.dcb.exception.UnauthorizedException;
import org.folio.dcb.integration.keycloak.model.DcbHubKCCredentials;
import org.folio.dcb.integration.keycloak.model.KeycloakAuthentication;
import org.folio.dcb.utils.KeyCloakTokenRequestHelper;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class KeycloakService {
  private final KeycloakClient keycloakClient;

  /**
   * Retrieves a user token using the provided DcbHubKCCredentials.
   *
   * @param dcbHubKCCredentials the credentials for the DCB Hub Keycloak
   * @return KeycloakAuthentication containing the token information
   */
  public KeycloakAuthentication getUserToken(DcbHubKCCredentials dcbHubKCCredentials) {
    var requestData = KeyCloakTokenRequestHelper.preparePasswordRequestBody(dcbHubKCCredentials);
    return getToken(requestData, dcbHubKCCredentials);
  }

  /**
   * Retrieves a KeycloakAuthentication token using the provided credentials and DcbHubKCCredentials.
   *
   * @param payload             the payload containing credentials
   * @param dcbHubKCCredentials the credentials for the DCB Hub Keycloak url and realm
   * @return KeycloakAuthentication containing the token information
   */
  private KeycloakAuthentication getToken(Map<String, String> payload, DcbHubKCCredentials dcbHubKCCredentials) {
    try {
      return keycloakClient.callTokenEndpoint(buildKeyCloakUri(dcbHubKCCredentials), payload);
    } catch (FeignException.Unauthorized e) {
      throw new UnauthorizedException("Unauthorized error while fetching token from keyCloak server", e);
    } catch (FeignException cause) {
      throw new ServiceException("Failed to obtain a token from KeyCloak server", cause);
    }
  }

  private static @NotNull URI buildKeyCloakUri(DcbHubKCCredentials dcbHubKCCredentials) {
    return fromUriString(dcbHubKCCredentials.getKeycloakUrl()).build().toUri();
  }
}
