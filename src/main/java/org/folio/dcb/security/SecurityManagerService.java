package org.folio.dcb.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dcb.domain.dto.PermissionUser;
import org.folio.dcb.domain.dto.Personal;
import org.folio.dcb.domain.dto.SystemUserParameters;
import org.folio.dcb.domain.dto.User;
import org.folio.dcb.domain.dto.UserType;
import org.folio.dcb.service.PermissionUserService;
import org.folio.dcb.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Log4j2
@RequiredArgsConstructor
public class SecurityManagerService {

  private static final String PERMISSIONS_FILE_PATH = "permissions/system-user-permissions.csv";
  private static final String USER_LAST_NAME = "SystemConsortia";

  private final PermissionUserService permissionUserService;
  private final AuthService authService;
  private final UserService userService;

  @Value("${folio.system.username}")
  private String username;

  @Value("${folio.system.password}")
  private String password;

  public void prepareSystemUser(String okapiUrl, String tenantId) {
    Optional<User> userOptional = userService.getByUsername(username);

    User user;
    if (userOptional.isPresent()) {
      user = userOptional.get();
      updateUser(user);
    } else {
      user = createUser(username);
      authService.saveCredentials(SystemUserParameters.builder()
        .id(UUID.randomUUID())
        .username(username)
        .password(password)
        .okapiUrl(okapiUrl)
        .tenant(tenantId)
        .build());
    }

    Optional<PermissionUser> permissionUserOptional = permissionUserService.getByUserId(user.getId());
    if (permissionUserOptional.isPresent()) {
      permissionUserService.addPermissions(permissionUserOptional.get(), PERMISSIONS_FILE_PATH);
    } else {
      permissionUserService.createWithPermissionsFromFile(user.getId(), PERMISSIONS_FILE_PATH);
    }
  }

  private User createUser(String username) {
    var result = createUserObject(username);
    userService.createUser(result);
    return result;
  }

  private void updateUser(User user) {
    if (existingUserUpToDate(user)) {
      log.info("{} is up to date.", user.getId());
    } else {
      populateMissingUserProperties(user);
      userService.updateUser(user);
    }
  }

  private User createUserObject(String username) {
    final var result = new User();

    result.setId(UUID.randomUUID().toString());
    result.setActive(true);
    result.setUsername(username);
    result.setType(UserType.SYSTEM.getName());

    populateMissingUserProperties(result);

    return result;
  }

  private boolean existingUserUpToDate(User user) {
    return user.getPersonal() != null && StringUtils.isNotBlank(user.getPersonal().getLastName());
  }

  private void populateMissingUserProperties(User user) {
    user.setType(UserType.SYSTEM.getName());
    user.setPersonal(new Personal());
    user.getPersonal().setLastName(USER_LAST_NAME);
  }

}
