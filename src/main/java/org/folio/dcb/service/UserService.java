package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.User;

public interface UserService {
  User createOrFetchUser(DcbPatron patronDetails);
}
