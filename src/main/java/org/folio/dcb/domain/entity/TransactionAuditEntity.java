package org.folio.dcb.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.dcb.domain.entity.base.TransactionAuditableEntity;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions_audit")
public class TransactionAuditEntity extends TransactionAuditableEntity {

  /**
   * Unique identifier for this audit record.
   *
   * <p>Generated automatically using UUID generation strategy.</p>
   */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /**
   * Description of the action performed on the transaction.
   *
   * <p>Examples: "CREATED", "UPDATED", "COMPLETED", "CANCELLED", etc.</p>
   */
  private String action;

  /**
   * JSON representation of the transaction state before the action was performed.
   *
   * <p>
   * Stored as PostgreSQL JSONB type for efficient querying and storage. May be null if the record represents a creation
   * action or if the previous state was not captured.
   * </p>
   */
  @ColumnTransformer(write = "?::jsonb")
  @Column(columnDefinition = "jsonb")
  private String before;

  /**
   * JSON representation of the transaction state after the action was performed.
   *
   * <p>
   * Stored as PostgreSQL JSONB type for efficient querying and storage. May be null if the action failed before any
   * state change occurred.
   * </p>
   */
  @ColumnTransformer(write = "?::jsonb")
  @Column(columnDefinition = "jsonb")
  private String after;

  /**
   * Error message if the action failed during processing.
   *
   * <p>
   * Contains the exception message or error description if an error occurred. Null if the action completed
   * successfully.
   * </p>
   */
  private String errorMessage;

  /**
   * Identifier of the transaction being audited.
   *
   * <p>
   * References the transaction that this audit record is tracking changes for.
   * </p>
   */
  private String transactionId;
}
