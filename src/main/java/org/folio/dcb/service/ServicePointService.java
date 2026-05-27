package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.ServicePointRequest;

public interface ServicePointService {
  ServicePointRequest createServicePointIfNotExists(DcbTransaction dcbTransaction);
}
