package org.folio.dcb.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  private String action;
  @ColumnTransformer(write = "?::jsonb")
  @Column(columnDefinition = "jsonb")
  private String before;
  @ColumnTransformer(write = "?::jsonb")
  @Column(columnDefinition = "jsonb")
  private String after;

  private String transactionId;
}
