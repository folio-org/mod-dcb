package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.User;

public interface RequestService {
  /**
   * Create page item request
   * @param user - userEntity
   * @param dcbItem - dcbItemEntity
   * @return
   */
  void createPageItemRequest(User user, DcbItem dcbItem);
}
