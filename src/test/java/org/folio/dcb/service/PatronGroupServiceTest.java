package org.folio.dcb.service;

import static org.folio.dcb.utils.EntityUtils.createUserGroupCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.dcb.integration.users.GroupClient;
import org.folio.dcb.service.impl.PatronGroupServiceImpl;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import static org.mockito.ArgumentMatchers.anyString;
import org.folio.dcb.domain.dto.UserGroupCollection;
import org.folio.dcb.domain.dto.UserGroup;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class PatronGroupServiceTest {

  @InjectMocks private PatronGroupServiceImpl patronGroupService;
  @Mock private GroupClient groupClient;

  @Test
  void fetchPatronGroupIdByNameTest() {
    var userGroupCollection = createUserGroupCollection();
    when(groupClient.fetchGroupByName(any())).thenReturn(userGroupCollection);
    var response = patronGroupService.fetchPatronGroupIdByName("staff");
    verify(groupClient).fetchGroupByName("group==\"staff\"");
    assertEquals(userGroupCollection.getUsergroups().getFirst().getId(), response);
  }

  @Test
  void fetchPatronGroupIdByInvalidNameTest() {
    var userGroupCollection = createUserGroupCollection();
    when(groupClient.fetchGroupByName(any())).thenReturn(userGroupCollection);
    assertThrows(NotFoundException.class, () -> patronGroupService.fetchPatronGroupIdByName("invalid"));
  }

    @Test
  void fetchPatronGroupIdByEmptyResultsTest() {
    // TestMate-b010671f8c95ebf096d763f89b1d27ad
    // Given
    String groupName = "unknown";
    UserGroupCollection emptyCollection = new UserGroupCollection();
    emptyCollection.setUsergroups(Collections.emptyList());
    emptyCollection.setTotalRecords(0);
    when(groupClient.fetchGroupByName(anyString())).thenReturn(emptyCollection);
    // When
    assertThrows(NotFoundException.class, () -> patronGroupService.fetchPatronGroupIdByName(groupName));
    // Then
    verify(groupClient).fetchGroupByName("group==\"unknown\"");
  }

    @Test
  void fetchPatronGroupIdByComplexNameTest() {
    // TestMate-53e3140766520ed0043bb0570807c250
    // Given
    String groupName = "Faculty Staff";
    String expectedId = "44963503-625d-453b-9a87-c838e553940c";
    UserGroupCollection userGroupCollection = createUserGroupCollection();
    UserGroup userGroup = new UserGroup();
    userGroup.setGroup(groupName);
    userGroup.setId(expectedId);
    userGroupCollection.setUsergroups(List.of(userGroup));
    when(groupClient.fetchGroupByName(anyString())).thenReturn(userGroupCollection);
    // When
    var response = patronGroupService.fetchPatronGroupIdByName(groupName);
    // Then
    verify(groupClient).fetchGroupByName("group==\"Faculty Staff\"");
    assertEquals(expectedId, response);
  }
}
