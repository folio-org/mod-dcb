package org.folio.dcb.integration.dcb.model;

import java.util.Date;
import lombok.Data;

@Data
public class DcbLocation {
  private String id;
  private Date dateCreated;
  private Date dateUpdated;
  private String code;
  private String name;
  private String type;
  private DcbAgency agency;
}
