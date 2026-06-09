package org.folio.dcb.domain.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.dcb.domain.converter.UuidConverter;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus.StatusEnum;
import org.folio.dcb.domain.entity.base.AuditableEntity;
import org.folio.dcb.repository.listener.TransactionAuditEntityListener;

@Entity
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions")
@EntityListeners(TransactionAuditEntityListener.class)
public class TransactionEntity extends AuditableEntity implements Serializable {

  /**
   * The saved state of this transaction entity used for tracking changes. This field is not persisted and is used
   * internally for audit purposes.
   */
  @Transient
  protected TransactionEntity savedState;

  /**
   * The unique identifier for the transaction.
   */
  @Id
  private String id;

  /**
   * The UUID of the item involved in the transaction.
   */
  @Convert(converter = UuidConverter.class)
  private String itemId;

  /**
   * The title of the item being transacted.
   */
  private String itemTitle;

  /**
   * The barcode of the item being transacted.
   */
  private String itemBarcode;

  /**
   * The unique identifier of the service point associated with this transaction.
   */
  private String servicePointId;

  /**
   * The name of the service point associated with this transaction.
   */
  private String servicePointName;

  /**
   * The material type of the item (e.g., Book, Journal, Video).
   */
  private String materialType;

  /**
   * The library code of the library responsible for the pickup of the item.
   */
  private String pickupLibraryCode;

  /**
   * The library code of the library lending the item.
   */
  private String lendingLibraryCode;

  /**
   * The UUID of the patron involved in the transaction.
   */
  @Convert(converter = UuidConverter.class)
  private String patronId;

  /**
   * The group or category to which the patron belongs.
   */
  private String patronGroup;

  /**
   * The barcode assigned to the patron.
   */
  private String patronBarcode;

  /**
   * The unique identifier of the request associated with this transaction.
   */
  private UUID requestId;

  /**
   * The current status of the transaction (e.g., OPEN, IN_TRANSIT, RECEIVED, CANCELLED).
   */
  @Enumerated(EnumType.STRING)
  private StatusEnum status;

  /**
   * The role of the institution in this transaction (e.g., BORROWER, LENDER, PICKUP).
   */
  @Enumerated(EnumType.STRING)
  private DcbTransaction.RoleEnum role;

  /**
   * Flag indicating whether this transaction represents a self-borrowing scenario where the patron borrows from their
   * own library.
   */
  private Boolean selfBorrowing;

  /**
   * The location code where the item is physically located or should be located.
   */
  private String itemLocationCode;
}
