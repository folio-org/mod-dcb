package org.folio.dcb.domain.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.DcbPickup;
import org.folio.dcb.domain.dto.TransactionStatusResponseList;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionMapper {

  private final ObjectMapper objectMapper;

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

  public List<TransactionStatusResponseList> mapToDto(Page<TransactionAuditEntity> transactionAuditEntityPage) {
    var transactionList = transactionAuditEntityPage.getContent();
    return transactionList
      .stream()
      .map(transactionAuditEntity -> getTransactionEntity(objectMapper, transactionAuditEntity))
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

  private TransactionEntity getTransactionEntity(ObjectMapper mapper, TransactionAuditEntity transactionAuditEntity) {
    try {
      return mapper.readValue(transactionAuditEntity.getAfter(), TransactionEntity.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public DcbItem mapTransactionEntityToDcbItem(TransactionEntity transactionEntity) {
    return DcbItem
      .builder()
      .id(transactionEntity.getItemId())
      .title(transactionEntity.getItemTitle())
      .barcode(transactionEntity.getItemBarcode())
      .materialType(transactionEntity.getMaterialType())
      .lendingLibraryCode(transactionEntity.getLendingLibraryCode())
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

}
