package org.folio.dcb.domain.mapper;

import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

  public TransactionEntity mapToEntity(String transactionId, DcbTransaction dcbTransaction) {
    if(dcbTransaction == null || dcbTransaction.getItem() == null || dcbTransaction.getPatron() == null) {
      return null;
    }

    var item = dcbTransaction.getItem();
    var patron = dcbTransaction.getPatron();
    return TransactionEntity.builder()
      .id(transactionId)
      .itemId(item.getId())
      .itemBarcode(item.getBarcode())
      .itemTitle(item.getTitle())
      .pickupLocation(item.getPickupLocation())
      .materialType(item.getMaterialType())
      .lendingLibraryCode(item.getLendingLibraryCode())
      .patronBarcode(patron.getBarcode())
      .patronId(patron.getId())
      .patronGroup(patron.getGroup())
      .borrowingLibraryCode(patron.getBorrowingLibraryCode())
      .build();
  }

}
