package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbItem;

public interface CirculationItemService {

  DcbItem fetchOrCreateItem(DcbItem dcbTransaction);
}
