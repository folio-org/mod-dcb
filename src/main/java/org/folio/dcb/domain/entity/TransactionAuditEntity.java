package org.folio.dcb.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.dcb.domain.entity.base.TransactionAuditableEntity;
import org.hibernate.annotations.ColumnTransformer;

import java.util.UUID;

@Entity
@Table(name = "transactions_audit")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionAuditEntity extends TransactionAuditableEntity {
  @Id
  private UUID id;
  private String action;

  @Column(columnDefinition = "jsonb")
  @ColumnTransformer(write = "?::jsonb")
  private String before;
  @Column(columnDefinition = "jsonb")
  @ColumnTransformer(write = "?::jsonb")
  private String after;

  private String transactionId;

}
