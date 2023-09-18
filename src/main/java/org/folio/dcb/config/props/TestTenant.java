package org.folio.dcb.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties("test-tenant")
@Component
public class TestTenant {
  private String tenantName;
}
