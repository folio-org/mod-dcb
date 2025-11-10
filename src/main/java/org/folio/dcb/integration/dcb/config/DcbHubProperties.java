package org.folio.dcb.integration.dcb.config;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.folio.common.configuration.properties.TlsProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "application.dcb-hub")
public class DcbHubProperties {

  private TlsProperties tls;
  private String locationsUrl;

  @NestedConfigurationProperty
  private boolean fetchDcbLocationsEnabled = false;

  @AssertTrue(message = "dcb-hub.locations-url must be provided when dcb-hub.fetch-dcb-locations-enabled is true")
  public boolean isLocationsUrl() {
    if (fetchDcbLocationsEnabled) {
      return StringUtils.isNotBlank(locationsUrl);
    }
    return true;
  }
}
