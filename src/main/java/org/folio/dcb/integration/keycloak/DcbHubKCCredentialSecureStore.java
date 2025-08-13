package org.folio.dcb.integration.keycloak;

import org.folio.dcb.integration.keycloak.model.DcbHubKCCredentials;
import org.folio.dcb.utils.JsonUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.config.properties.FolioEnvironment;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.SecretNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DcbHubKCCredentialSecureStore {

  private final SecureStore secureStore;
  private final FolioEnvironment folioEnvironment;
  private final FolioExecutionContext folioExecutionContext;

  @Value("${application.dcb-hub.secure-store-keyname}")
  private String dcbHubSecureStoreKeyName;

  public DcbHubKCCredentials getDcbHubKCCredentials() {
    var dcbHubCredentials = retrieveDcbHubCredentials();
    return JsonUtils.jsonToObject(dcbHubCredentials, DcbHubKCCredentials.class);
  }

  private String retrieveDcbHubCredentials() {
    try {
      var tenantId = folioExecutionContext.getTenantId();
      return secureStore.get(buildKey(dcbHubSecureStoreKeyName, folioEnvironment.getEnvironment(), tenantId));
    } catch (SecretNotFoundException e) {
      throw new IllegalStateException(
        "Failed to get DCB Hub credentials from secure store", e);
    }
  }

  private String buildKey(String dcbHubKeyName, String env, String tenantId) {
    return String.format("%s_%s_%s", env, tenantId, dcbHubKeyName);
  }
}
