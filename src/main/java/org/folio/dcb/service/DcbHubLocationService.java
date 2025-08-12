package org.folio.dcb.service;

import org.folio.dcb.domain.dto.ServicePointRequest;

public interface DcbHubLocationService {
  void createShadowLocations(ServicePointRequest servicePointRequest);
}
