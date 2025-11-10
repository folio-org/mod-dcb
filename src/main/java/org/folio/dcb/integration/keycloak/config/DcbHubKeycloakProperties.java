package org.folio.dcb.integration.keycloak.config;

import org.folio.common.configuration.properties.TlsProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "application.keycloak")
public class DcbHubKeycloakProperties {

  @NestedConfigurationProperty
  private TlsProperties tls;
}
