package org.folio.dcb.domain.mapper;

import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.DcbPickup;
import org.folio.dcb.domain.dto.DcbUpdateItem;
import org.folio.dcb.domain.dto.TransactionStatusResponseList;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.utils.JsonUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import java.util.List;

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
      .selfBorrowing(dcbTransaction.getSelfBorrowing())
      .itemLocationCode(item.getLocationCode())
      .build();
  }

  public List<TransactionStatusResponseList> mapToDto(Page<TransactionAuditEntity> transactionAuditEntityPage) {
    var transactionList = transactionAuditEntityPage.getContent();
    return transactionList
      .stream()
      .map(transactionAuditEntity -> JsonUtils.jsonToObject(transactionAuditEntity.getAfter(), TransactionEntity.class))
      .map(transactionEntity -> TransactionStatusResponseList
        .builder()
        .id(transactionEntity.getId())
        .pickup(mapTransactionEntityToDcbPickup(transactionEntity))
        .item(mapTransactionEntityToDcbItem(transactionEntity))
        .patron(mapTransactionEntityToDcbPatron(transactionEntity))
        .status(TransactionStatusResponseList.StatusEnum.fromValue
          (transactionEntity.getStatus().getValue()))
        .role(TransactionStatusResponseList.RoleEnum.fromValue
          (transactionEntity.getRole().getValue()))
        .build())
      .toList();

  }

  public DcbItem mapTransactionEntityToDcbItem(TransactionEntity transactionEntity) {
    return DcbItem
      .builder()
      .id(transactionEntity.getItemId())
      .title(transactionEntity.getItemTitle())
      .barcode(transactionEntity.getItemBarcode())
      .materialType(transactionEntity.getMaterialType())
      .lendingLibraryCode(transactionEntity.getLendingLibraryCode())
      .locationCode(transactionEntity.getItemLocationCode())
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

  public DcbPickup mapTransactionEntityToDcbPickup(TransactionEntity transactionEntity) {
    return DcbPickup
      .builder()
      .servicePointId(transactionEntity.getServicePointId())
      .servicePointName(transactionEntity.getServicePointName())
      .libraryCode(transactionEntity.getPickupLibraryCode())
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

}
