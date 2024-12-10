package org.folio.dcb.utils;

import lombok.SneakyThrows;
import org.folio.dcb.DcbApplication;
import org.folio.dcb.client.feign.HoldingsStorageClient;
import org.folio.dcb.domain.dto.Calendar;
import org.folio.dcb.domain.dto.CalendarCollection;
import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.DcbPickup;
import org.folio.dcb.domain.dto.DcbUpdateTransaction;
import org.folio.dcb.domain.dto.DcbUpdateItem;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.dto.User;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.entity.TransactionAuditEntity;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.dto.InventoryItem;
import org.folio.dcb.domain.dto.UserGroupCollection;
import org.folio.dcb.domain.dto.UserGroup;
import org.folio.dcb.domain.dto.UserCollection;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.folio.dcb.service.impl.ServicePointServiceImpl.HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT;

public class EntityUtils {

  public static String ITEM_ID = "5b95877d-86c0-4cb7-a0cd-7660b348ae5a";

  /**
   * NOT_EXISTED_PATRON_ID - means
   * the Mocked userClient returns empty result,
   * while requesting it by the query, including such a patron id
   * */
  public static String NOT_EXISTED_PATRON_ID = "571b0a2c-9456-40b5-a449-d41fe6017082";

  /**
   * EXISTED_PATRON_ID - means
   * the Mocked userClient returns result with single value,
   * while requesting it by the query, including such a patron id
   * */
  public static String EXISTED_PATRON_ID = "284056f5-0670-4e1e-9e2f-61b9f1ee2d18";
  public static String PICKUP_SERVICE_POINT_ID = "0da8c1e4-1c1f-4dd9-b189-70ba978b7d94";
  public static String DCB_TRANSACTION_ID = "571b0a2c-8883-40b5-a449-d41fe6017082";
  public static String CIRCULATION_REQUEST_ID = "571b0a2c-8883-40b5-a449-d41fe6017083";

  public static String CIRCULATION_ITEM_REQUEST_ID = "571b0a2c-8883-40b5-a449-d41fe6017183";
  public static String DCB_USER_TYPE = "dcb";
  public static String DCB_TYPE_USER_ID = "910c512c-ebc5-40c6-96a5-a20bfd81e154";
  public static String EXISTED_INVENTORY_ITEM_BARCODE = "INVENTORY_ITEM";
  public static String PATRON_TYPE_USER_ID = "18c1741d-e678-4c8e-9fe7-cfaeefab5eea";
  public static String REQUEST_ID = "398501a2-5c97-4ba6-9ee7-d1cd6433cb98";
  public static String DCB_NEW_BARCODE = "NEW_BARCODE";

  public static DcbTransaction createDcbTransactionByRole(DcbTransaction.RoleEnum role) {
    return DcbTransaction.builder()
      .item(createDcbItem())
      .patron(switch (role){
        case BORROWING_PICKUP, BORROWER -> createDcbPatronWithExactPatronId(EXISTED_PATRON_ID);
        default -> createDefaultDcbPatron();
        }
      )
      .role(role)
      .pickup(createDcbPickup())
      .build();
  }

  public static DcbTransaction createLendingEcsRequestTransactionByRole() {
    return DcbTransaction.builder()
      .requestId(REQUEST_ID)
      .role(DcbTransaction.RoleEnum.LENDER)
      .pickup(createDcbPickup())
      .build();
  }

  public static DcbTransaction createBorrowingEcsRequestTransactionByRole() {
    return DcbTransaction.builder()
      .requestId(REQUEST_ID)
      .item(createDcbItem())
      .role(DcbTransaction.RoleEnum.BORROWER)
      .pickup(createDcbPickup())
      .build();
  }

  public static org.folio.dcb.domain.dto.ServicePointRequest createServicePointRequest() {
    return org.folio.dcb.domain.dto.ServicePointRequest.builder()
      .id(PICKUP_SERVICE_POINT_ID)
      .name("DCB_TestLibraryCode_TestServicePointCode")
      .code("DCB_TESTLIBRARYCODE_TESTSERVICEPOINTCODE")
      .discoveryDisplayName("DCB_TestLibraryCode_TestServicePointCode")
      .pickupLocation(true)
      .holdShelfExpiryPeriod(org.folio.dcb.domain.dto.HoldShelfExpiryPeriod.builder().duration(3).intervalId(org.folio.dcb.domain.dto.HoldShelfExpiryPeriod.IntervalIdEnum.DAYS).build())
      .holdShelfClosedLibraryDateManagement(HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT)
      .build();
  }

  public static TransactionStatus createTransactionStatus(TransactionStatus.StatusEnum statusEnum){
    return TransactionStatus.builder().status(statusEnum).build();
  }

  public static DcbItem createDcbItem() {
    return DcbItem.builder()
      .id(ITEM_ID)
      .barcode("DCB_ITEM")
      .title("ITEM")
      .lendingLibraryCode("KU")
      .materialType("book")
      .build();
  }

  public static DcbUpdateTransaction createDcbTransactionUpdate() {
    return DcbUpdateTransaction
      .builder()
      .item(DcbUpdateItem
        .builder()
        .barcode(DCB_NEW_BARCODE)
        .lendingLibraryCode("LEN")
        .materialType("DVD")
        .build())
      .build();
  }

  public static CirculationRequest createCirculationRequest() {
    return CirculationRequest.builder()
      .id(CIRCULATION_REQUEST_ID)
      .build();
  }

  public static CirculationItem createCirculationItem() {
    return CirculationItem.builder()
      .id(CIRCULATION_ITEM_REQUEST_ID)
      .build();
  }

  public static DcbPatron createDcbPatronWithExactPatronId(String patronId) {
    return DcbPatron.builder()
      .id(patronId)
      .barcode("DCB_PATRON")
      .group("staff")
      .build();
  }
  public static DcbPatron createDefaultDcbPatron() {
    return createDcbPatronWithExactPatronId(NOT_EXISTED_PATRON_ID);
  }

  public static org.folio.dcb.domain.dto.DcbPickup createDcbPickup() {
    return DcbPickup.builder()
      .servicePointId(PICKUP_SERVICE_POINT_ID)
      .servicePointName("TestServicePointCode")
      .libraryCode("TestLibraryCode")
      .build();
  }

  public static TransactionStatusResponse createTransactionResponse() {
    return TransactionStatusResponse.builder()
      .patron(createDefaultDcbPatron())
      .item(createDcbItem())
      .status(TransactionStatusResponse.StatusEnum.CREATED)
      .build();
  }

  public static User createUser() {
    return User.builder()
      .active(true)
      .patronGroup("staff")
      .id(NOT_EXISTED_PATRON_ID)
      .type(DCB_USER_TYPE)
      .build();
  }

  public static UserCollection createUserCollection() {
    return UserCollection.builder()
      .users(List.of(createUser()))
      .totalRecords(1)
      .build();
  }

  public static TransactionEntity createTransactionEntity() {
    return TransactionEntity.builder()
      .id(DCB_TRANSACTION_ID)
      .itemId(ITEM_ID)
      .itemTitle("ITEM TITLE")
      .itemBarcode("DCB_ITEM")
      .patronId(NOT_EXISTED_PATRON_ID)
      .patronBarcode("DCB_PATRON")
      .patronGroup("staff")
      .servicePointId(PICKUP_SERVICE_POINT_ID)
      .servicePointName("TestServicePointCode")
      .pickupLibraryCode("TestLibraryCode")
      .materialType("book")
      .lendingLibraryCode("LEN")
      .requestId(UUID.fromString(CIRCULATION_REQUEST_ID))
      .build();
  }

  public static HoldingsStorageClient.Holding createInventoryHolding() {
    return HoldingsStorageClient.Holding.builder()
      .id(UUID.randomUUID().toString())
      .instanceId(UUID.randomUUID().toString())
      .build();
  }

  @SneakyThrows
  public static String getMockDataAsString(String path) {

    try (InputStream resourceAsStream = DcbApplication.class.getClassLoader().getResourceAsStream(path)) {
      if (resourceAsStream != null) {
        return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
      } else {
        StringBuilder sb = new StringBuilder();
        try (Stream<String> lines = Files.lines(Paths.get(path))) {
          lines.forEach(sb::append);
        }
        return sb.toString();
      }
    }
  }

  public static InventoryItem createInventoryItem() {
    return InventoryItem.builder()
      .id(UUID.randomUUID().toString())
      .holdingsRecordId(UUID.randomUUID().toString())
      .barcode("DCB_ITEM")
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

  public static TransactionAuditEntity createTransactionAuditEntity(){
    return TransactionAuditEntity.builder()
      .id(UUID.randomUUID())
      .transactionId(UUID.randomUUID().toString())
      .action("UPDATE")
      .before("")
      .after("")
      .build();
  }

  public static CalendarCollection getCalendarCollection(String calendarName) {
    var calendar = Calendar.builder()
      .name(calendarName)
      .startDate(LocalDate.now().toString())
      .endDate(LocalDate.now().plusYears(10).toString())
      .assignments(new ArrayList<>())
      .build();
    var calendarCollection = new CalendarCollection();
    calendarCollection.setCalendars(List.of(calendar));
    calendarCollection.setTotalRecords(1);
    return calendarCollection;
  }
}
