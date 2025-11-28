package org.folio.dcb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("application.retry.item")
public class ItemRetryConfiguration {

  /**
   * The maximum number of retry attempts for item-related operations.
   */
  private int maxRetries = 5;

  /**
   * The interval duration between retry attempts.
   */
  private long delayMilliseconds = 250;
}
