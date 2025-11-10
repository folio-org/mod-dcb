package org.folio.dcb.integration.dcb.model;

import java.util.ArrayList;
import lombok.Data;

@Data
public class Sort {
  private ArrayList<OrderBy> orderBy;
}
