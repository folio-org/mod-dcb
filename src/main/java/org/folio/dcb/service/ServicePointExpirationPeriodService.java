package org.folio.dcb.service;

import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;

public interface ServicePointExpirationPeriodService {

  /**
   * Retrieves the hold shelf expiry period for a given settings key.
   *
   * @param settingKey the settings key for the service point
   * @return the hold shelf expiry period
   */
  HoldShelfExpiryPeriod getShelfExpiryPeriod(String settingKey);

  /**
   * Constructs the settings key for a service point using the given prefix.
   *
   * @param prefix the prefix for the service point
   * @return the settings key in the format "{prefix}.hold-shelf-expiry-period"
   */
  static String getSettingsKey(String prefix) {
    return "%s.hold-shelf-expiry-period".formatted(prefix.toLowerCase());
  }
}
