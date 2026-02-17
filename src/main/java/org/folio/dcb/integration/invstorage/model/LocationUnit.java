package org.folio.dcb.integration.invstorage.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationUnit {

  private String id;
  private String name;
  private String code;
  private String institutionId;
  private String campusId;
  private String libraryId;

  @Builder.Default
  @JsonProperty("isShadow")
  @Getter(onMethod_ = @JsonIgnore)
  private boolean isShadow = false;
}
