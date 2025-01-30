package org.folio.dcb.domain.entity;

import java.io.Serializable;
import java.util.UUID;

import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.IntervalIdEnum;
import org.folio.dcb.domain.dto.TransactionStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "service_point_expiration_period")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServicePointExpirationPeriodEntity implements Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  private Integer duration;
  @Enumerated(EnumType.STRING)
  private IntervalIdEnum intervalId;
}
