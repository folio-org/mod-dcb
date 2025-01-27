package org.folio.dcb.service;

import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.User;

public interface RequestService {
  /**
   * Create request based on status of the item
   * @param user - userEntity
   * @param dcbItem - dcbItemEntity
   */
  CirculationRequest createRequestBasedOnItemStatus(User user, DcbItem dcbItem, String pickupServicePointId);
  CirculationRequest createHoldItemRequest(User user, DcbItem dcbItem, String pickupServicePointId);
}
