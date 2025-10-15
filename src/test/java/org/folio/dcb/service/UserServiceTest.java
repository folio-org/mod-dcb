package org.folio.dcb.service;

import org.folio.dcb.client.feign.UsersClient;
import org.folio.dcb.domain.dto.Personal;
import org.folio.dcb.domain.dto.User;
import org.folio.dcb.domain.dto.UserCollection;
import org.folio.dcb.service.impl.PatronGroupServiceImpl;
import org.folio.dcb.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.folio.dcb.utils.EntityUtils.createDefaultDcbPatron;
import static org.folio.dcb.utils.EntityUtils.createUser;
import static org.folio.dcb.utils.EntityUtils.createUserCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks private UserServiceImpl userService;
  @Mock private UsersClient usersClient;
  @Mock private PatronGroupServiceImpl patronGroupService;
  @Captor private ArgumentCaptor<User> userCaptor;

  @Test
  void fetchUserTestWithSameGroup() {
    var userCollection = createUserCollection();
    var groupId = UUID.randomUUID().toString();
    userCollection.getUsers().getFirst().setPatronGroup(groupId);
    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(userCollection);
    when(patronGroupService.fetchPatronGroupIdByName("staff"))
      .thenReturn(groupId);
    userService.fetchOrCreateUser(createDefaultDcbPatron());
    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
    verify(usersClient, never()).updateUser(any(), any());
    verify(usersClient, never()).createUser(any());
  }

  @Test
  void fetchUserTestWithDifferentGroup() {
    var userCollection = createUserCollection();
    var groupId = UUID.randomUUID().toString();
    userCollection.getUsers().getFirst().setPatronGroup(groupId);
    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(userCollection);
    when(patronGroupService.fetchPatronGroupIdByName("staff"))
      .thenReturn(UUID.randomUUID().toString());
    userService.fetchOrCreateUser(createDefaultDcbPatron());
    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
    verify(usersClient).updateUser(any(), any());
    verify(usersClient, never()).createUser(any());
  }

  @Test
  void createUserTest() {
    var expectedUser = createUser();
    var userCollection = new UserCollection();
    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(userCollection);
    when(usersClient.createUser(userCaptor.capture())).thenReturn(expectedUser);
    when(patronGroupService.fetchPatronGroupIdByName(any())).thenReturn(UUID.randomUUID().toString());

    var result = userService.fetchOrCreateUser(createDefaultDcbPatron());

    var capturedUserData = userCaptor.getValue();
    assertEquals(expectedUser, result);
    assertNotNull(capturedUserData);
    assertEquals(new Personal().lastName("DcbSystem"), capturedUserData.getPersonal());

    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
    verify(usersClient).createUser(any());
  }

  @Test
  void createUserTestWithLocalNamesProvided() {
    var expectedUser = createUser();
    var userCollection = new UserCollection();
    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(userCollection);
    when(usersClient.createUser(userCaptor.capture())).thenReturn(expectedUser);
    when(patronGroupService.fetchPatronGroupIdByName(any())).thenReturn(UUID.randomUUID().toString());

    var givenDcbPatron = createDefaultDcbPatron().localNames("[John, Doe]");
    var result = userService.fetchOrCreateUser(givenDcbPatron);

    var capturedUserData = userCaptor.getValue();
    var expectedPersonal = new Personal().firstName("John").lastName("Doe");

    assertEquals(expectedUser, result);
    assertNotNull(capturedUserData);
    assertEquals(expectedPersonal, capturedUserData.getPersonal());

    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
    verify(usersClient).createUser(any());
  }
}
