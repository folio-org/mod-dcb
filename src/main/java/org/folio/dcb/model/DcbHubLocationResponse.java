package org.folio.dcb.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class DcbHubLocationResponse {
  private List<Location> content;
  private Pageable pageable;
  private Integer pageNumber;
  private Integer offset;
  private Integer size;
  private Boolean empty;
  private Integer numberOfElements;
  private Integer totalSize;
  private Integer totalPages;

  @Data
  public static class Location {
    private String id;
    private Date dateCreated;
    private Date dateUpdated;
    private String code;
    private String name;
    private String type;
    private Agency agency;
  }

  @Data
  public static class Agency {
    private String id;
    private Date dateCreated;
    private Date dateUpdated;
    private String code;
    private String name;
  }

  @Data
  public static class Pageable {
    private ArrayList<OrderBy> orderBy;
    private Integer number;
    private Integer size;
    private Sort sort;
  }

  @Data
  public static class Sort {
    private ArrayList<OrderBy> orderBy;
  }

  @Data
  public static class OrderBy {
    private Boolean ignoreCase;
    private String direction;
    private String property;
    private Boolean ascending;
  }
}