package org.folio.dcb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "application.features")
public class DcbFeatureProperties {

  /**
   * Configuration toggle for shadow locations feature.
   */
  private boolean flexibleCirculationRulesEnabled = false;
}
