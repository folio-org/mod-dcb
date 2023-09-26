package org.folio.dcb.service;

import org.folio.dcb.domain.entity.TransactionEntity;

public interface CirculationService {

  void checkInByBarcode(TransactionEntity dcbTransaction);

}
