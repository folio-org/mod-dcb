package org.folio.dcb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.folio.dcb.utils.EntityUtils.createUserGroupCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.dcb.domain.dto.UserGroup;
import org.folio.dcb.domain.dto.UserGroupCollection;
import org.folio.dcb.integration.users.GroupClient;
import org.folio.dcb.service.impl.PatronGroupServiceImpl;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    assertThat(userGroupCollection.getUsergroups().getFirst().getId()).isEqualTo(response);
  }

  @Test
  void fetchPatronGroupIdByInvalidNameTest() {
    var userGroupCollection = createUserGroupCollection();
    when(groupClient.fetchGroupByName(any())).thenReturn(userGroupCollection);
    assertThatThrownBy(() -> patronGroupService.fetchPatronGroupIdByName("invalid"))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  void fetchPatronGroupIdByEmptyResultsTest() {
    // TestMate-b010671f8c95ebf096d763f89b1d27ad
    var groupName = "unknown";
    var emptyCollection = new UserGroupCollection().totalRecords(0);
    when(groupClient.fetchGroupByName(anyString())).thenReturn(emptyCollection);

    assertThatThrownBy(() -> patronGroupService.fetchPatronGroupIdByName(groupName))
      .isInstanceOf(NotFoundException.class);
    verify(groupClient).fetchGroupByName("group==\"unknown\"");
  }

  @Test
  void fetchPatronGroupIdByComplexNameTest() {
    // TestMate-53e3140766520ed0043bb0570807c250
    var groupName = "Faculty Staff";
    var expectedId = "44963503-625d-453b-9a87-c838e553940c";
    var userGroupCollection = createUserGroupCollection();
    var userGroup = new UserGroup().group(groupName).id(expectedId);
    userGroupCollection.setUsergroups(List.of(userGroup));
    when(groupClient.fetchGroupByName(anyString())).thenReturn(userGroupCollection);

    var response = patronGroupService.fetchPatronGroupIdByName(groupName);

    verify(groupClient).fetchGroupByName("group==\"Faculty Staff\"");
    assertEquals(expectedId, response);
  }
}
