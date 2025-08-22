package org.folio.dcb.service;

import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.DcbItem;

public interface CirculationItemService {

  CirculationItem checkIfItemExistsAndCreate(DcbItem dcbTransaction, String pickupServicePointId, String locationCode);

  CirculationItem fetchItemById(String itemId);

}
