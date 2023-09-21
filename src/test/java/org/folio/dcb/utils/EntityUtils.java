package org.folio.dcb.utils;

import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.dto.User;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.dto.InventoryHolding;
import org.folio.dcb.domain.dto.InventoryItem;
import org.folio.dcb.domain.dto.UserGroupCollection;
import org.folio.dcb.domain.dto.UserGroup;
import org.folio.dcb.domain.dto.UserCollection;

import java.util.List;
import java.util.UUID;

public class EntityUtils {

  public static String ITEM_ID = "5b95877d-86c0-4cb7-a0cd-7660b348ae5a";
  public static String PATRON_ID = "571b0a2c-9456-40b5-a449-d41fe6017082";
  public static String DCB_TRANSACTION_ID = "571b0a2c-8883-40b5-a449-d41fe6017082";

  public static DcbTransaction createDcbTransaction() {
    return DcbTransaction.builder()
      .item(createDcbItem())
      .patron(createDcbPatron())
      .role(DcbTransaction.RoleEnum.LENDING)
      .build();
  }

  public static DcbItem createDcbItem() {
    return DcbItem.builder()
      .id(ITEM_ID)
      .barcode("DCB_ITEM")
      .title("ITEM")
      .lendingLibraryCode("KU")
      .pickupLocation("Datalogisk Institute")
      .materialType("book")
      .build();
  }

  public static DcbPatron createDcbPatron() {
    return DcbPatron.builder()
      .id(PATRON_ID)
      .barcode("DCB_PATRON")
      .group("staff")
      .borrowingLibraryCode("E")
      .build();
  }

  public static TransactionStatusResponse createTransactionResponse() {
    return TransactionStatusResponse.builder()
      .patron(createDcbPatron())
      .item(createDcbItem())
      .status(TransactionStatusResponse.StatusEnum.CREATED)
      .build();
  }

  public static User createUser() {
    return User.builder()
      .active(true)
      .patronGroup("staff")
      .id(PATRON_ID)
      .build();
  }

  public static UserCollection createUserCollection() {
    return UserCollection.builder()
      .users(List.of(createUser()))
      .build();
  }

  public static TransactionEntity createTransactionEntity() {
    return TransactionEntity.builder()
      .id(DCB_TRANSACTION_ID)
      .itemId(ITEM_ID)
      .itemBarcode("DCB_ITEM")
      .patronId(PATRON_ID)
      .patronBarcode("DCB_PATRON")
      .patronGroup("staff")
      .build();
  }

  public static InventoryHolding createInventoryHolding() {
    return InventoryHolding.builder()
      .id(UUID.randomUUID().toString())
      .instanceId(UUID.randomUUID().toString())
      .build();
  }

  public static InventoryItem createInventoryItem() {
    return InventoryItem.builder()
      .id(UUID.randomUUID().toString())
      .holdingsRecordId(UUID.randomUUID().toString())
      .build();
  }

  public static UserGroupCollection createUserGroupCollection() {
    return UserGroupCollection.builder()
      .usergroups(List.of(createUserGroup()))
      .totalRecords(1)
      .build();
  }

  private static UserGroup createUserGroup() {
    return UserGroup.builder()
      .id(UUID.randomUUID().toString())
      .group("staff")
      .desc("staff group")
      .build();
  }

}
