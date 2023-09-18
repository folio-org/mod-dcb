package org.folio.dcb.client;

import org.folio.dcb.domain.dto.Permission;
import org.folio.dcb.domain.dto.PermissionUser;
import org.folio.dcb.domain.dto.PermissionUserCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("perms/users")
public interface PermissionsClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  PermissionUserCollection get(@RequestParam("query") String query);

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  PermissionUser create(@RequestBody PermissionUser permissionUser);

  @PostMapping(value = "/{userId}/permissions?indexField=userId", consumes = MediaType.APPLICATION_JSON_VALUE)
  void addPermission(@PathVariable("userId") String userId, Permission permission);

  @DeleteMapping(value = "/{id}")
  void deletePermissionUser(@PathVariable("id") String id);
}
