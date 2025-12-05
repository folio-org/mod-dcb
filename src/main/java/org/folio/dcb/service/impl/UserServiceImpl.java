package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.dcb.client.feign.UsersClient;
import org.folio.dcb.domain.DcbPersonal;
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
    }

    return modifyAndGetExistingUser(patronDetails, user);
  }

  private User modifyAndGetExistingUser(DcbPatron patronDetails, User user) {
    validateDcbUserType(user.getType());
    var isUserGroupChanged =  updateUserGroup(patronDetails, user);
    var isUserPersonalInfoChanged = updateUserPersonal(patronDetails, user);
    if (isUserGroupChanged || isUserPersonalInfoChanged) {
      usersClient.updateUser(user.getId(), user);
    }

    return user;
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
    var personalData = DcbPersonal.parseLocalNames(patron.getLocalNames());
    return User.builder()
      .active(true)
      .barcode(patron.getBarcode())
      .patronGroup(groupId)
      .id(patron.getId())
      .type(DCB_TYPE)
      .personal(getUserPersonalInfo(personalData))
      .build();
  }

  private boolean updateUserGroup(DcbPatron patronDetails, User user) {
    var groupId = patronGroupService.fetchPatronGroupIdByName(patronDetails.getGroup());
    if (!groupId.equals(user.getPatronGroup())) {
      log.info("updateUserGroup:: updating patron group from {} to {} for user with barcode {}",
        user.getPatronGroup(), groupId, user.getBarcode());
      user.setPatronGroup(groupId);
      return true;
    }

    return false;
  }

  private boolean updateUserPersonal(DcbPatron patronDetails, User user) {
    var newPersonalData = DcbPersonal.parseLocalNames(patronDetails.getLocalNames());
    if (newPersonalData.isDefault()) {
      return false;
    }

    var newUserPersonal = getUserPersonalInfo(newPersonalData);
    if (Objects.equals(user.getPersonal(), newUserPersonal)) {
     return false;
    }

    log.info("updateUserPersonal:: updating personal data for user with barcode {}", user.getBarcode());
    user.setPersonal(newUserPersonal);
    return true;
  }

  private void validateDcbUserType(String userType) {
    if(ObjectUtils.notEqual(userType, DCB_TYPE) && ObjectUtils.notEqual(userType, SHADOW_TYPE)) {
      throw new IllegalArgumentException(String.format("User with type %s is retrieved. so unable to create transaction", userType));
    }
  }

  private static Personal getUserPersonalInfo(DcbPersonal personalData) {
    return Personal.builder()
      .firstName(personalData.getFirstName())
      .middleName(personalData.getMiddleName())
      .lastName(personalData.getLastName())
      .build();
  }
}
