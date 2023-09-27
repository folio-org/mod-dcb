package org.folio.dcb.service;

public interface PatronGroupService {
  /**
   * Get patron group id by groupName
   * @param groupName - name of group
   * @return id
   */
  String fetchPatronGroupIdByName(String groupName);
}
