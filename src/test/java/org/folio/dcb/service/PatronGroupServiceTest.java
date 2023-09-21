package org.folio.dcb.service;

import org.folio.dcb.client.feign.GroupClient;
import org.folio.dcb.service.impl.PatronGroupServiceImpl;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.folio.dcb.utils.EntityUtils.createUserGroupCollection;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatronGroupServiceTest {

  @InjectMocks
  private PatronGroupServiceImpl patronGroupService;
  @Mock
  private GroupClient groupClient;

  @Test
  void fetchPatronGroupIdByNameTest() {
    var userGroupCollection = createUserGroupCollection();
    when(groupClient.fetchGroupByName(any())).thenReturn(userGroupCollection);
    var response = patronGroupService.fetchPatronGroupIdByName("staff");
    verify(groupClient).fetchGroupByName("group==staff");
    assertEquals(userGroupCollection.getUsergroups().get(0).getId(), response);
  }

  @Test
  void fetchPatronGroupIdByInvalidNameTest() {
    var userGroupCollection = createUserGroupCollection();
    when(groupClient.fetchGroupByName(any())).thenReturn(userGroupCollection);
    assertThrows(NotFoundException.class, () -> patronGroupService.fetchPatronGroupIdByName("invalid"));
  }
}
