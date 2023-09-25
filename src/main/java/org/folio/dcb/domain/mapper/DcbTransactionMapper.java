package org.folio.dcb.domain.mapper;

import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.DcbItem;

import org.springframework.stereotype.Component;

@Component
public class DcbTransactionMapper {
  public DcbTransaction mapToDcbTransaction(TransactionEntity transactionEntity) {
    DcbItem dcbItem = DcbItem.builder()
      .id(transactionEntity.getItemId())
      .barcode(transactionEntity.getItemBarcode())
      .title(transactionEntity.getItemTitle())
      .pickupLocation(transactionEntity.getPickupLocation())
      .materialType(transactionEntity.getMaterialType())
      .lendingLibraryCode(transactionEntity.getLendingLibraryCode())
      .build();

    DcbPatron dcbPatron = DcbPatron.builder()
      .id(transactionEntity.getPatronId())
      .barcode(transactionEntity.getPatronBarcode())
      .group(transactionEntity.getPatronGroup())
      .borrowingLibraryCode(transactionEntity.getBorrowingLibraryCode())
      .build();


    DcbTransaction.RoleEnum role = transactionEntity.getRole();

    return  DcbTransaction.builder()
      .item(dcbItem)
      .patron(dcbPatron)
      .role(role)
      .build();
  }

  }
