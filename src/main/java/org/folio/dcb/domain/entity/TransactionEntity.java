package org.folio.dcb.domain.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.entity.base.AuditableEntity;
import org.folio.dcb.domain.dto.TransactionStatus.StatusEnum;
import org.folio.dcb.listener.entity.TransactionAuditEntityListener;

@Entity
@Table(name = "transactions")
@EntityListeners(TransactionAuditEntityListener.class)
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
  private String servicePointId;
  private String servicePointName;
  private String materialType;
  private String pickupLibraryName;
  private String pickupLibraryCode;
  private String lendingLibraryCode;
  @Convert(converter = UUIDConverter.class)
  private String patronId;
  private String patronGroup;
  private String patronBarcode;
  private String borrowingLibraryCode;
  @Enumerated(EnumType.STRING)
  private StatusEnum status;
  @Enumerated(EnumType.STRING)
  private DcbTransaction.RoleEnum role;


}
