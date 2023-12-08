package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.UsersClient;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.Personal;
import org.folio.dcb.domain.dto.User;
import org.folio.dcb.service.PatronGroupService;
import org.folio.dcb.service.UserService;
import org.folio.spring.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static org.folio.dcb.utils.DCBConstants.DCB_TYPE;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserServiceImpl implements UserService {

  private final PatronGroupService patronGroupService;
  private final UsersClient usersClient;
  private static final String LAST_NAME = "DcbSystem";

  @Override
  public User fetchUser(DcbPatron dcbPatron) {
    var dcbPatronBarcode = dcbPatron.getBarcode();

    log.debug("fetchUser:: Fetching user by userBarcode {}.", dcbPatronBarcode);
    var user = fetchUserByBarcode(dcbPatronBarcode);

    if(Objects.isNull(user)) {
      log.error("fetchUser:: Unable to find existing user with barcode {}.", dcbPatronBarcode);
      throw new NotFoundException(String.format("Unable to find existing user with barcode %s.", dcbPatronBarcode));
    }

    return user;
  }

  public User fetchOrCreateUser(DcbPatron patronDetails) {
    log.debug("createOrFetchUser:: Trying to create or find user for userBarcode {}",
      patronDetails.getBarcode());
    var user = fetchUserByBarcode(patronDetails.getBarcode());
    if(Objects.isNull(user)) {
      log.info("fetchOrCreateUser:: Unable to find existing user with barcode {} and id {}. Hence, creating new user",
        patronDetails.getBarcode(), patronDetails.getId());
      user = createUser(patronDetails);
    }
    return user;
  }

  private User createUser(DcbPatron patronDetails) {
    log.debug("createUser:: creating new user with id {} and barcode {}",
      patronDetails.getId(), patronDetails.getBarcode());
    var groupId = patronGroupService.fetchPatronGroupIdByName(patronDetails.getGroup());
    return usersClient.createUser(createVirtualUser(patronDetails, groupId));
  }

  private User fetchUserByBarcode(String barcode) {
    log.debug("fetchUserByBarcodeAndId:: Trying to fetch existing user with barcode {} ",
      barcode);
    return usersClient.fetchUserByBarcode("barcode==" + barcode)
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
}
