package org.folio.dcb.service;

import java.util.Optional;

import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.DcbItem;

public interface CirculationItemService {

  CirculationItem checkIfItemExistsAndCreate(DcbItem dcbTransaction, String pickupServicePointId);
  Optional<CirculationItem> fetchCirculationItemByBarcode(String barcode);
  CirculationItem fetchItemById(String itemId);
  CirculationItem createCirculationItem(DcbItem item, String pickupServicePointId);
}
