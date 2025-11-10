package org.folio.dcb.integration.dcb.model;

import java.util.ArrayList;
import lombok.Data;

@Data
public class Pageable {
  private ArrayList<OrderBy> orderBy;
  private Integer number;
  private Integer size;
  private Sort sort;
}
