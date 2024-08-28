package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbPickup;
import org.folio.dcb.domain.dto.ServicePointRequest;

public interface ServicePointService {
  ServicePointRequest createServicePointIfNotExists(DcbPickup pickupServicePoint);
}
