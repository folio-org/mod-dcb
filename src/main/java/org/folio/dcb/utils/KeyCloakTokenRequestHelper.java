package org.folio.dcb.utils;

import static java.util.Map.entry;

import java.util.Map;

import org.folio.dcb.integration.keycloak.model.DcbHubKCCredentials;

import lombok.experimental.UtilityClass;

@UtilityClass
public class KeyCloakTokenRequestHelper {
  private static final String GRANT_TYPE = "grant_type";
  private static final String CLIENT_ID = "client_id";
  private static final String CLIENT_SECRET = "client_secret";
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";

  public static Map<String, String> preparePasswordRequestBody(DcbHubKCCredentials dcbHubKCCredentials) {
    return Map.ofEntries(
      entry(GRANT_TYPE, PASSWORD),
      entry(CLIENT_ID, dcbHubKCCredentials.getClientId()),
      entry(USERNAME, dcbHubKCCredentials.getUsername()),
      entry(PASSWORD, dcbHubKCCredentials.getPassword()),
      entry(CLIENT_SECRET, dcbHubKCCredentials.getClientSecret() != null ? dcbHubKCCredentials.getClientSecret() : ""));
  }
}
