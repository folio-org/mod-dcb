package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.UsersClient;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.User;
import org.folio.dcb.service.PatronGroupService;
import org.folio.dcb.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserServiceImpl implements UserService {

  private final PatronGroupService patronGroupService;
  private final UsersClient usersClient;

  public User fetchOrCreateUser(DcbPatron patronDetails) {
    log.debug("createOrFetchUser:: Trying to create or find user for userId {}, userBarcode {}",
      patronDetails.getId(), patronDetails.getBarcode());
    var user = fetchUserByBarcodeAndId(patronDetails.getBarcode(), patronDetails.getId());
    if(Objects.isNull(user)) {
      log.info("fetchOrCreateUser:: Unable to find existing user with barcode {} and id {}. Hence, creating new user",
        patronDetails.getId(), patronDetails.getBarcode());
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
      .build();
  }
}
