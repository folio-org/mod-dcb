package org.folio.dcb.domain.mapper;

import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.DcbUpdateItem;
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
      .patronBarcode(patron.getBarcode())
      .patronId(patron.getId())
      .patronGroup(patron.getGroup())

      .role(dcbTransaction.getRole())
      .build();
  }

  public DcbItem convertTransactionUpdateItemToDcbItem(DcbUpdateItem dcbUpdateItem, TransactionEntity entity) {
    return DcbItem
      .builder()
      .lendingLibraryCode(dcbUpdateItem.getLendingLibraryCode())
      .barcode(dcbUpdateItem.getBarcode())
      .materialType(dcbUpdateItem.getMaterialType())
      .title(entity.getItemTitle())
      .build();
  }
  public DcbPatron mapTransactionEntityToDcbPatron(TransactionEntity transactionEntity) {
    return DcbPatron
      .builder()
      .id(transactionEntity.getPatronId())
      .group(transactionEntity.getPatronGroup())
      .barcode(transactionEntity.getPatronBarcode())
      .build();
  }
}
