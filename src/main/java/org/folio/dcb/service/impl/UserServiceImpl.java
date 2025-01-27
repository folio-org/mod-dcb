package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.dcb.client.feign.UsersClient;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.Personal;
import org.folio.dcb.domain.dto.User;
import org.folio.dcb.service.PatronGroupService;
import org.folio.dcb.service.UserService;
import org.folio.spring.exception.NotFoundException;
import org.folio.util.StringUtil;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static org.folio.dcb.utils.DCBConstants.DCB_TYPE;
import static org.folio.dcb.utils.DCBConstants.SHADOW_TYPE;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserServiceImpl implements UserService {

  private final PatronGroupService patronGroupService;
  private final UsersClient usersClient;
  private static final String LAST_NAME = "DcbSystem";

  @Override
  public User fetchUser(DcbPatron dcbPatron) {
    var dcbPatronId = dcbPatron.getId();
    var dcbPatronBarcode = dcbPatron.getBarcode();

    log.debug("fetchUser:: Fetching user by userId {}, userBarcode {}.", dcbPatronId, dcbPatronBarcode);
    var user = fetchUserByBarcodeAndId(dcbPatronBarcode, dcbPatronId);

    if(Objects.isNull(user)) {
      log.error("fetchUser:: Unable to find existing user with barcode {} and id {}.", dcbPatronBarcode, dcbPatronId);
      throw new NotFoundException(String.format("Unable to find existing user with barcode %s and id %s.", dcbPatronBarcode, dcbPatronId));
    }

    return user;
  }

  public User fetchOrCreateUser(DcbPatron patronDetails) {
    log.debug("createOrFetchUser:: Trying to create or find user for userId {}, userBarcode {}",
      patronDetails.getId(), patronDetails.getBarcode());
    var user = fetchUserByBarcodeAndId(patronDetails.getBarcode(), patronDetails.getId());
    if(Objects.isNull(user)) {
      log.info("fetchOrCreateUser:: Unable to find existing user with barcode {} and id {}. Hence, creating new user",
        patronDetails.getBarcode(), patronDetails.getId());
      return createUser(patronDetails);
    } else {
      validateDcbUserType(user.getType());
      var groupId = patronGroupService.fetchPatronGroupIdByName(patronDetails.getGroup());
      if (!groupId.equals(user.getPatronGroup())) {
        return updateUserGroup(user, groupId);
      }
      return user;
    }
  }

  private User createUser(DcbPatron patronDetails) {
    log.debug("createUser:: creating new user with id {} and barcode {}",
      patronDetails.getId(), patronDetails.getBarcode());
    var groupId = patronGroupService.fetchPatronGroupIdByName(patronDetails.getGroup());
    return usersClient.createUser(createVirtualUser(patronDetails, groupId));
  }

  private User fetchUserByBarcodeAndId(String barcode, String id) {
    log.debug("fetchUserByBarcodeAndId:: Trying to fetch existing user with barcode {} and id {}",
      barcode, id);
    return usersClient.fetchUserByBarcodeAndId("barcode==" + StringUtil.cqlEncode(barcode) + " and id==" + StringUtil.cqlEncode(id))
      .getUsers()
      .stream()
      .findFirst()
      .orElse(null);
  }

  private User createVirtualUser(DcbPatron patron, String groupId) {
    return User.builder()
      .active(true)
      .barcode(patron.getBarcode())
      .patronGroup(groupId)
      .id(patron.getId())
      .type(DCB_TYPE)
      .personal(Personal.builder().lastName(LAST_NAME).build())
      .build();
  }

  private User updateUserGroup(User user, String patronGroupId) {
    log.info("updatePatronGroup:: updating patron group from {} to {} for user with barcode {}",
      user.getPatronGroup(), patronGroupId, user.getBarcode());
    user.setPatronGroup(patronGroupId);
    usersClient.updateUser(user.getId(), user);
    return user;
  }

  private void validateDcbUserType(String userType) {
    if(ObjectUtils.notEqual(userType, DCB_TYPE) && ObjectUtils.notEqual(userType, SHADOW_TYPE)) {
      throw new IllegalArgumentException(String.format("User with type %s is retrieved. so unable to create transaction", userType));
    }
  }
}
