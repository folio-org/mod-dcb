package org.folio.dcb.integration.dcb.model;

import java.util.List;

import lombok.Data;

@Data
public class DcbHubLocationResponse {
  private List<DcbLocation> content;
  private Pageable pageable;
  private Integer pageNumber;
  private Integer offset;
  private Integer size;
  private Boolean empty;
  private Integer numberOfElements;
  private Integer totalSize;
  private Integer totalPages;
}
