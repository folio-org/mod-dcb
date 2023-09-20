package org.folio.dcb.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.exception.ResourceNotFoundException;
import org.folio.dcb.client.feign.UsersClient;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.User;
import org.folio.dcb.service.PatronGroupService;
import org.folio.dcb.service.UserService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserServiceImpl implements UserService {

  private final PatronGroupService patronGroupService;
  private final UsersClient usersClient;

  public User createOrFetchUser(DcbPatron patronDetails) {
    log.debug("createOrFetchUser:: Trying to create or find user for userId {}, userBarcode {}",
      patronDetails.getId(), patronDetails.getBarcode());
    try {
      return createUser(patronDetails);
    } catch (FeignException.UnprocessableEntity ex) {
      log.warn("createOrFetchUser:: Exception occurs while creating new user {} ", ex.getMessage());
      return fetchUserByBarcodeAndId(patronDetails.getBarcode(), patronDetails.getId());
    }
  }

  private User createUser(DcbPatron patronDetails) {
    log.info("createUser:: creating new user with id {} and barcode {}",
      patronDetails.getId(), patronDetails.getBarcode());
    var groupId = patronGroupService.fetchPatronGroupIdByName(patronDetails.getGroup());
    return usersClient.createUser(prepareUser(patronDetails, groupId));
  }

  private User fetchUserByBarcodeAndId(String barcode, String id) {
    log.debug("fetchUserByBarcodeAndId:: Trying to fetch existing user with barcode {} and id {}",
      barcode, id);
    return usersClient.fetchUserByBarcodeAndId("barcode==" + barcode + " and id==" + id)
      .getUsers()
      .stream()
      .findFirst()
      .orElseThrow(() -> new ResourceNotFoundException("unable to find User"));
  }

  private User prepareUser(DcbPatron patron, String groupId) {
    return User.builder()
      .active(true)
      .barcode(patron.getBarcode())
      .patronGroup(groupId)
      .id(patron.getId())
      .build();
  }
}
