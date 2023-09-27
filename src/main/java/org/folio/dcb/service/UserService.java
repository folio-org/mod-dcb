package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.User;

public interface UserService {
  /**
   * Get user or create an user if not found
   * @param dcbPatron - dcbPatronEntity
   * @return user
   */
  User fetchOrCreateUser(DcbPatron dcbPatron);
}
