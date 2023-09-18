package org.folio.dcb.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class PermissionUserCollection {

  private List<PermissionUser> permissionUsers;
  private Integer totalRecords;

}
