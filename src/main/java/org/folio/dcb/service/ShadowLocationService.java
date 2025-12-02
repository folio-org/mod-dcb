package org.folio.dcb.service;

import org.folio.dcb.domain.dto.RefreshShadowLocationResponse;
import org.folio.dcb.domain.dto.ShadowLocationRefreshBody;

public interface ShadowLocationService {
  /**
   * Creates shadow locations based on the provided request body.
   *
   * @param requestBody the request body containing shadow location refresh details
   * @return a response containing the result of the shadow location creation
   */
  RefreshShadowLocationResponse createShadowLocations(ShadowLocationRefreshBody requestBody);
}
