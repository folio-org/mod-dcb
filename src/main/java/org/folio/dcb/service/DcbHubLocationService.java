package org.folio.dcb.service;

import org.folio.dcb.client.feign.LocationUnitClient;
import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.domain.dto.ServicePointRequest;

public interface DcbHubLocationService {
  void createShadowLocations(LocationsClient locationsClient, LocationUnitClient locationUnitClient,
    ServicePointRequest servicePointRequest);
}
