package org.folio.dcb.domain.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.dcb.domain.converter.PostgresUUIDConverter;
import org.folio.dcb.domain.dto.TransactionStatus;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transactions")
public class Transactions {


  @Id
  @Convert(converter = PostgresUUIDConverter.class)
  private UUID id;
  @Convert(converter = PostgresUUIDConverter.class)
  private UUID itemId;

  private String itemTitle;
  private String itemBarcode;
  private String pickupLocation;
  private String materialType;
  private String lendingLibraryCode;

  @Convert(converter = PostgresUUIDConverter.class)
  private UUID patronId;
  private String patronGroup;
  private String patronBarcode;
  private String borrowingLibraryCode;

  @Enumerated(EnumType.STRING)
  private TransactionStatus.StatusEnum status;
}
