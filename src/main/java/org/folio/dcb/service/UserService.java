package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.User;

public interface UserService {

  /**
   * Get a user or create one if not found.
   *
   * @param dcbPatron - dcbPatronEntity
   * @return user
   */
  User fetchOrCreateUser(DcbPatron dcbPatron);

  /**
   * Retrieve an existing user.
   *
   * <p>It is strongly expected that the input parameter references an existing user.
   *
   * @param dcbPatron - dcbPatronEntity
   * @return user
   */
  User fetchUser(DcbPatron dcbPatron);
}
