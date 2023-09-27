package org.folio.dcb.domain.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.dcb.domain.converter.UUIDConverter;
import org.folio.dcb.domain.dto.Role.TransactionRoleEnum;
import org.folio.dcb.domain.entity.base.AuditableEntity;
import org.folio.dcb.domain.dto.TransactionStatus.StatusEnum;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionEntity extends AuditableEntity {
  @Id
  private String id;
  @Convert(converter = UUIDConverter.class)
  private String itemId;
  private String itemTitle;
  private String itemBarcode;
  private String pickupLocation;
  private String materialType;
  private String lendingLibraryCode;
  @Convert(converter = UUIDConverter.class)
  private String patronId;
  private String patronGroup;
  private String patronBarcode;
  private String borrowingLibraryCode;
  @Enumerated(EnumType.STRING)
  private StatusEnum status;
  @Enumerated(EnumType.STRING)
  private TransactionRoleEnum role;

}
