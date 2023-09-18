package org.folio.dcb.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class Personal {
  @JsonProperty("lastName")
  private String lastName;

  @JsonProperty("firstName")
  private String firstName;

  @JsonProperty("middleName")
  private String middleName;

  @JsonProperty("preferredFirstName")
  private String preferredFirstName;

  @JsonProperty("email")
  private String email;

  @JsonProperty("phone")
  private String phone;

  @JsonProperty("mobilePhone")
  private String mobilePhone;

  @JsonProperty("dateOfBirth")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Date dateOfBirth;

  @JsonProperty("addresses")
  @Valid
  private List<Address> addresses = null;

  @JsonProperty("preferredContactTypeId")
  private String preferredContactTypeId;
}
