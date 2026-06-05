package org.folio.dcb.service;

public interface PatronGroupService {
  /**
   * Get patron group ID by groupName.
   *
   * @param groupName - name of group
   * @return id
   */
  String fetchPatronGroupIdByName(String groupName);
}
