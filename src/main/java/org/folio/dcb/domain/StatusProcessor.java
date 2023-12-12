package org.folio.dcb.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.folio.dcb.domain.dto.TransactionStatus.StatusEnum;

@AllArgsConstructor
@Data
public class StatusProcessor {

  private StatusEnum currentStatus;
  private StatusEnum nextStatus;
  private boolean manual;
  private StatusProcessor nextProcessor;

}
