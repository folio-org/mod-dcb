package org.folio.dcb.service;

import org.folio.dcb.domain.dto.CirculationItemRequest;
import org.folio.dcb.domain.dto.DcbItem;

public interface CirculationItemService {

  void checkIfItemExistsAndCreate(DcbItem dcbTransaction);
  CirculationItemRequest fetchItemById(String itemId);

}
