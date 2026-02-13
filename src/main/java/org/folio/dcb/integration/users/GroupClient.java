package org.folio.dcb.integration.users;

import org.folio.dcb.domain.dto.UserGroupCollection;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("groups")
public interface GroupClient {

  @GetExchange
  UserGroupCollection fetchGroupByName(@RequestParam("query") String query);
}
