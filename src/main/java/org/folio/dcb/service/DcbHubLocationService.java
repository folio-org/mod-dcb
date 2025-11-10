package org.folio.dcb.service;

import org.folio.dcb.domain.dto.RefreshShadowLocationResponse;

public interface DcbHubLocationService {
  RefreshShadowLocationResponse createShadowLocations(boolean isTenantInitRequest);
}
