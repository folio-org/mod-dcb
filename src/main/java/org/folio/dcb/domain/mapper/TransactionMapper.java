package org.folio.dcb.domain.mapper;

import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

  public TransactionEntity mapToEntity(String transactionId, DcbTransaction dcbTransaction) {
    if(dcbTransaction == null || dcbTransaction.getItem() == null || dcbTransaction.getPatron() == null || dcbTransaction.getPickup() == null) {
      return null;
    }

    var item = dcbTransaction.getItem();
    var patron = dcbTransaction.getPatron();
    var pickup = dcbTransaction.getPickup();

    return TransactionEntity.builder()
      .id(transactionId)
      .itemId(item.getId())
      .itemBarcode(item.getBarcode())
      .itemTitle(item.getTitle())
      .materialType(item.getMaterialType())
      .lendingLibraryCode(item.getLendingLibraryCode())
      .servicePointId(pickup.getServicePointId())
      .servicePointName(pickup.getServicePointName())
      .pickupLibraryCode(pickup.getLibraryCode())
      .pickupLibraryName(pickup.getLibraryName())
      .patronBarcode(patron.getBarcode())
      .patronId(patron.getId())
      .patronGroup(patron.getGroup())
      .borrowingLibraryCode(patron.getBorrowingLibraryCode())
      .agencyCode(item.getAgencyCode())

      .role(dcbTransaction.getRole())
      .build();
  }

}
