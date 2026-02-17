package org.folio.dcb.integration.users;

import org.folio.dcb.domain.dto.User;
import org.folio.dcb.domain.dto.UserCollection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange("users")
public interface UsersClient {

  @PostExchange
  User createUser(@RequestBody User user);

  @GetExchange
  UserCollection fetchUserByBarcodeAndId(@RequestParam("query") String query);

  @PutExchange("/{userId}")
  void updateUser(@PathVariable String userId, @RequestBody User user);
}
