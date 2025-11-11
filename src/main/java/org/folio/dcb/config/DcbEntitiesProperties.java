package org.folio.dcb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "application.dcb-entities")
public class DcbEntitiesProperties {

  /**
   * Defines if runtime verification of entity presence in the system is required.
   *
   * <p>If entity is not present - it will be automatically created.</p>
   */
  private boolean runtimeVerificationEnabled = false;
}
