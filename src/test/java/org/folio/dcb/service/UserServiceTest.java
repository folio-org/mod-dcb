package org.folio.dcb.service;

import org.folio.dcb.client.feign.UsersClient;
import org.folio.dcb.service.impl.PatronGroupServiceImpl;
import org.folio.dcb.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.folio.dcb.utils.EntityUtils.createDcbPatron;
import static org.folio.dcb.utils.EntityUtils.createUserCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks
  private UserServiceImpl userService;
  @Mock
  private UsersClient usersClient;
  @Mock
  private PatronGroupServiceImpl patronGroupService;

  @Test
  void fetchUserTest() {
    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(createUserCollection());
    userService.fetchOrCreateUser(createDcbPatron());
    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService, never()).fetchPatronGroupIdByName("staff");
    verify(usersClient, never()).createUser(any());
  }

  @Test
  void createUserTest() {
    var userCollection = createUserCollection();
    userCollection.setUsers(List.of());
    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(userCollection);
    when(patronGroupService.fetchPatronGroupIdByName(any())).thenReturn(UUID.randomUUID().toString());
    userService.fetchOrCreateUser(createDcbPatron());
    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
    verify(usersClient).createUser(any());
  }
}
