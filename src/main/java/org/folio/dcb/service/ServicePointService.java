package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbPickup;
import org.folio.dcb.domain.dto.ServicePointRequest;

public interface ServicePointService {
  String HoldShelfClosedLibraryDateManagement = "Keep_the_current_due_date";
  ServicePointRequest createServicePoint(DcbPickup pickupServicePoint);
}
