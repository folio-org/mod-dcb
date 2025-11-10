package org.folio.dcb.integration.dcb.model;

import lombok.Data;

@Data
public class OrderBy {
  private Boolean ignoreCase;
  private String direction;
  private String property;
  private Boolean ascending;
}
