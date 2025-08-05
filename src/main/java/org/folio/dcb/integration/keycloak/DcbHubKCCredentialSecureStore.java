package org.folio.dcb.integration.keycloak;

import org.folio.dcb.integration.keycloak.model.DcbHubKCCredentials;
import org.folio.dcb.utils.JsonUtils;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.SecretNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DcbHubKCCredentialSecureStore {

  private final SecureStore secureStore;

  @Value("${application.dcb-hub.secure-store-keyname}")
  private String dcbHubSecureStoreKeyName;

  public DcbHubKCCredentials getDcbHubKCCredentials() {
    var dcbHubCredentials = retrieveDcbHubCredentials();
    return JsonUtils.jsonToObject(dcbHubCredentials, DcbHubKCCredentials.class);
  }

  private String retrieveDcbHubCredentials() {
    try {
      return secureStore.get(dcbHubSecureStoreKeyName);
    } catch (SecretNotFoundException e) {
      throw new IllegalStateException(
        "Failed to get DCB Hub credentials from secure store", e);
    }
  }
}
