package org.folio.dcb.integration.invstorage.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Location {

  private String id;
  private String name;
  private String code;
  private String institutionId;
  private String campusId;
  private String libraryId;
  private String primaryServicePoint;
  private List<String> servicePointIds;

  @Builder.Default
  @JsonProperty("isShadow")
  @Getter(onMethod_ = @JsonIgnore)
  private boolean isShadow = false;
}
