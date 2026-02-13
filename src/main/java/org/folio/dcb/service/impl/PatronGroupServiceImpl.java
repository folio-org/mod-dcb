package org.folio.dcb.service.impl;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.UserGroup;
import org.folio.dcb.integration.users.GroupClient;
import org.folio.dcb.service.PatronGroupService;
import org.folio.spring.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class PatronGroupServiceImpl implements PatronGroupService {

  private final GroupClient groupClient;

  @Override
  public String fetchPatronGroupIdByName(String groupName) {
    log.debug("fetchPatronGroupIdByName:: Fetching patron group details with groupName {} ",
      groupName);
    return groupClient.fetchGroupByName(getQueryByGroupName(groupName))
      .getUsergroups()
      .stream()
      .filter(group -> Objects.equals(group.getGroup(), groupName))
      .findFirst()
      .map(UserGroup::getId)
      .orElseThrow(() -> new NotFoundException(String.format("Patron group not found with name %s ", groupName)));
  }

  private String getQueryByGroupName(String groupName) {
    return "group==\"" + groupName + "\"";
  }
}
