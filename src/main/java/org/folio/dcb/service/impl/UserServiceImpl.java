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

@Service
@RequiredArgsConstructor
@Log4j2
public class UserServiceImpl implements UserService {

  private final PatronGroupService patronGroupService;
  private final UsersClient usersClient;
  private static final String DCB = "dcb";
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

  private User fetchUserByBarcodeAndId(String barcode, String id) {
    log.debug("fetchUserByBarcodeAndId:: Trying to fetch existing user with barcode {} and id {}",
      barcode, id);
    return usersClient.fetchUserByBarcodeAndId("barcode==" + barcode + " and id==" + id)
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
      .type(DCB)
      .personal(Personal.builder().lastName(LAST_NAME).build())
      .build();
  }
}
