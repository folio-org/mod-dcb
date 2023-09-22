package org.folio.dcb.service;

import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.User;

public interface RequestService {
  void createPageItemRequest(User user, DcbItem item);
}
