package org.folio.dcb.service;

import org.folio.dcb.client.feign.UsersClient;
import org.folio.dcb.domain.dto.Personal;
import org.folio.dcb.domain.dto.User;
import org.folio.dcb.domain.dto.UserCollection;
import org.folio.dcb.service.impl.PatronGroupServiceImpl;
import org.folio.dcb.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.utils.EntityUtils.DCB_USER_TYPE;
import static org.folio.dcb.utils.EntityUtils.createDefaultDcbPatron;
import static org.folio.dcb.utils.EntityUtils.createUser;
import static org.folio.dcb.utils.EntityUtils.dcbPatron;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks private UserServiceImpl userService;
  @Mock private UsersClient usersClient;
  @Mock private PatronGroupServiceImpl patronGroupService;
  @Captor private ArgumentCaptor<User> userCaptor;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(usersClient);
  }

  @Test
  void fetchOrCreateUser_positive_newVirtualUser() {
    var userId = randomUuid();
    var groupId = randomUuid();

    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(userCollection());
    when(patronGroupService.fetchPatronGroupIdByName(any())).thenReturn(groupId);
    when(usersClient.createUser(userCaptor.capture())).then(inv -> inv.getArgument(0));

    userService.fetchOrCreateUser(dcbPatron(userId));

    assertThat(userCaptor.getValue()).isEqualTo(virtualUser(userId, groupId));

    verify(usersClient).createUser(any());
    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
  }

  @Test
  void fetchOrCreateUser_positive_existingUserWithGroupChange() {
    var userId = randomUuid();
    var groupId = randomUuid();
    var newGroupId = randomUuid();
    var founduser = virtualUser(userId, groupId);

    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(userCollection(founduser));
    when(patronGroupService.fetchPatronGroupIdByName(any())).thenReturn(newGroupId);
    doNothing().when(usersClient).updateUser(eq(userId), userCaptor.capture());

    userService.fetchOrCreateUser(dcbPatron(userId));

    assertThat(userCaptor.getValue()).isEqualTo(virtualUser(userId, newGroupId));

    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
    verify(usersClient).updateUser(any(), any());
    verify(usersClient, never()).createUser(any());
  }

  @Test
  void fetchOrCreateUser_positive_existingUserWithoutGroupChange() {
    var userId = randomUuid();
    var groupId = randomUuid();
    var foundUsers = userCollection(virtualUser(userId, groupId));
    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(foundUsers);
    when(patronGroupService.fetchPatronGroupIdByName("staff")).thenReturn(groupId);

    userService.fetchOrCreateUser(dcbPatron(userId));

    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
    verify(usersClient, never()).updateUser(any(), any());
    verify(usersClient, never()).createUser(any());
  }

  @Test
  void findOrCreateUser_positive_newUserWithDefaultPersonalData() {
    var expectedUser = createUser();
    var userCollection = new UserCollection();
    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(userCollection);
    when(usersClient.createUser(userCaptor.capture())).thenReturn(expectedUser);
    when(patronGroupService.fetchPatronGroupIdByName(any())).thenReturn(UUID.randomUUID().toString());

    var result = userService.fetchOrCreateUser(createDefaultDcbPatron());

    assertThat(result).isEqualTo(expectedUser);
    assertThat(userCaptor.getValue()).isNotNull()
      .extracting(User::getPersonal)
      .isEqualTo(new Personal().lastName("DcbSystem"));

    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
    verify(usersClient).createUser(any());
  }

  @Test
  void findOrCreateUser_positive_newUserWithPersonalData() {
    var userId = randomUuid();
    var groupId = randomUuid();
    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(new UserCollection());
    when(usersClient.createUser(userCaptor.capture())).then(inv -> inv.getArgument(0));
    when(patronGroupService.fetchPatronGroupIdByName(any())).thenReturn(groupId);

    var givenDcbPatron = dcbPatron(userId, "[John, Doe]");
    var result = userService.fetchOrCreateUser(givenDcbPatron);

    assertThat(result).isEqualTo(virtualUser(userId, groupId, personalInfo("John", null, "Doe")));
    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
    verify(usersClient, never()).updateUser(any(), any());
    verify(usersClient).createUser(any());
  }

  @Test
  void fetchOrCreateUser_positive_existingUserWithPersonalDataChange() {
    var userId = randomUuid();
    var groupId = randomUuid();
    var foundUser = virtualUser(userId, groupId, personalInfo("John", "Michael", "Doe"));

    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(userCollection(foundUser));
    when(patronGroupService.fetchPatronGroupIdByName(any())).thenReturn(groupId);
    doNothing().when(usersClient).updateUser(eq(userId), userCaptor.capture());

    var dcbPatron = dcbPatron(userId, "[New-John, New-Michael, New-Doe]");
    var result = userService.fetchOrCreateUser(dcbPatron);

    var expectedPersonalInfo = personalInfo("New-John", "New-Michael", "New-Doe");
    var expectedUser = virtualUser(userId, groupId, expectedPersonalInfo);
    assertThat(result).isEqualTo(expectedUser);
    assertThat(userCaptor.getValue()).isEqualTo(expectedUser);
    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
    verify(usersClient).updateUser(any(), any());
    verify(usersClient, never()).createUser(any());
  }

  @Test
  void fetchOrCreateUser_positive_existingDefaultUserWithoutPersonalDataChange() {
    var userId = randomUuid();
    var groupId = randomUuid();
    var foundUser = virtualUser(userId, groupId, defaultPersonalInfo());
    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(userCollection(foundUser));
    when(patronGroupService.fetchPatronGroupIdByName(any())).thenReturn(groupId);

    var dcbPatron = dcbPatron(userId);
    var result = userService.fetchOrCreateUser(dcbPatron);

    assertThat(result).isEqualTo(virtualUser(userId, groupId, defaultPersonalInfo()));
    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
    verify(usersClient, never()).updateUser(any(), any());
    verify(usersClient, never()).createUser(any());
  }

  @Test
  void fetchOrCreateUser_positive_existingUserWithoutPersonalDataChange() {
    var userId = randomUuid();
    var groupId = randomUuid();
    var foundUser = virtualUser(userId, groupId, personalInfo("John", null, "Doe"));
    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(userCollection(foundUser));
    when(patronGroupService.fetchPatronGroupIdByName(any())).thenReturn(groupId);

    var dcbPatron = dcbPatron(userId, "[John, Doe]");
    var result = userService.fetchOrCreateUser(dcbPatron);

    assertThat(result).isEqualTo(virtualUser(userId, groupId, personalInfo("John", null, "Doe")));
    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
    verify(usersClient, never()).updateUser(any(), any());
    verify(usersClient, never()).createUser(any());
  }

  @Test
  void fetchOrCreateUser_positive_existingUserNotUpdatedWithDefaultValues() {
    var userId = randomUuid();
    var groupId = randomUuid();
    var foundUser = virtualUser(userId, groupId, personalInfo("John", null, "Doe"));
    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(userCollection(foundUser));
    when(patronGroupService.fetchPatronGroupIdByName(any())).thenReturn(groupId);

    var dcbPatron = dcbPatron(userId);
    var result = userService.fetchOrCreateUser(dcbPatron);

    assertThat(result).isEqualTo(virtualUser(userId, groupId, personalInfo("John", null, "Doe")));
    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
    verify(usersClient, never()).updateUser(any(), any());
    verify(usersClient, never()).createUser(any());
  }

  @Test
  void fetchOrCreateUser_positive_existingUserWithGroupAndPersonalInfoChange() {
    var userId = randomUuid();
    var groupId = randomUuid();
    var newGroupId = randomUuid();
    var foundUser = virtualUser(userId, groupId, personalInfo("John", null, "Doe"));
    when(usersClient.fetchUserByBarcodeAndId(any())).thenReturn(userCollection(foundUser));
    when(patronGroupService.fetchPatronGroupIdByName(any())).thenReturn(newGroupId);
    doNothing().when(usersClient).updateUser(eq(userId), userCaptor.capture());

    var dcbPatron = dcbPatron(userId, "[updJohn, updDoe]");
    var result = userService.fetchOrCreateUser(dcbPatron);

    var expectedUser = virtualUser(userId, newGroupId, personalInfo("updJohn", null, "updDoe"));
    assertThat(result).isEqualTo(expectedUser);
    assertThat(userCaptor.getValue()).isEqualTo(expectedUser);
    verify(usersClient).fetchUserByBarcodeAndId(any());
    verify(patronGroupService).fetchPatronGroupIdByName("staff");
    verify(usersClient).updateUser(any(), any());
    verify(usersClient, never()).createUser(any());
  }

  private static UserCollection userCollection(User...users) {
    return new UserCollection().users(List.of(users)).totalRecords(users.length);
  }

  public static User virtualUser(String id, String patronGroupId) {
    return virtualUser(id, patronGroupId, defaultPersonalInfo());
  }

  public static User virtualUser(String id, String patronGroupId, Personal personal) {
    return User.builder()
      .id(id)
      .active(true)
      .patronGroup(patronGroupId)
      .barcode("DCB_PATRON")
      .type(DCB_USER_TYPE)
      .personal(personal)
      .build();
  }

  public static Personal defaultPersonalInfo() {
    return new Personal()
      .lastName("DcbSystem");
  }

  public static String randomUuid() {
    return UUID.randomUUID().toString();
  }

  public static Personal personalInfo(String firstName, String middleName, String lastName) {
    return new Personal().firstName(firstName).middleName(middleName).lastName(lastName);
  }
}
