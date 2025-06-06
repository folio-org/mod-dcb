package org.folio.dcb.utils;

import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.IntervalIdEnum;
import org.folio.dcb.domain.dto.ItemStatus;

import java.util.List;

import static org.folio.dcb.domain.dto.ItemStatus.NameEnum.AWAITING_DELIVERY;
import static org.folio.dcb.domain.dto.ItemStatus.NameEnum.AWAITING_PICKUP;
import static org.folio.dcb.domain.dto.ItemStatus.NameEnum.CHECKED_OUT;
import static org.folio.dcb.domain.dto.ItemStatus.NameEnum.IN_TRANSIT;
import static org.folio.dcb.domain.dto.ItemStatus.NameEnum.PAGED;

public class DCBConstants {

  private DCBConstants() {}

  public static final String INSTANCE_ID = "9d1b77e4-f02e-4b7f-b296-3f2042ddac54";
  public static final String INSTANCE_TYPE_ID = "9d1b77e0-f02e-4b7f-b296-3f2042ddac54";
  public static final String INSTITUTION_ID = "9d1b77e5-f02e-4b7f-b296-3f2042ddac54";
  public static final String CAMPUS_ID = "9d1b77e6-f02e-4b7f-b296-3f2042ddac54";
  public static final String LIBRARY_ID = "9d1b77e7-f02e-4b7f-b296-3f2042ddac54";
  public static final String LOCATION_ID = "9d1b77e8-f02e-4b7f-b296-3f2042ddac54";
  public static final String SERVICE_POINT_ID = "9d1b77e8-f02e-4b7f-b296-3f2042ddac54";
  public static final String HOLDING_ID = "10cd3a5a-d36f-4c7a-bc4f-e1ae3cf820c9";
  public static final String CANCELLATION_REASON_ID = "50ed35b2-1397-4e83-a76b-642adf91ca2a";
  public static final String INSTANCE_TITLE = "DCB_INSTANCE";
  public static final String SOURCE = "FOLIO";
  public static final String INSTANCE_TYPE_SOURCE = "local";
  public static final String NAME = "DCB";
  public static final String CODE = "000";
  public static final String LOAN_TYPE_ID = "4dec5417-0765-4767-bed6-b363a2d7d4e2";
  public static final String DCB_LOAN_TYPE_NAME = "DCB Can circulate";
  public static final String MATERIAL_TYPE_NAME_BOOK = "book";
  public static final String DCB_CANCELLATION_REASON_NAME = "DCB Cancelled";
  public static final String DCB_TYPE = "dcb";
  public static final String SHADOW_TYPE = "shadow";
  public static final String HOLDING_SOURCE = "folio";
  public static final String DCB_CALENDAR_NAME = "DCB Calendar";
  public static final List<ItemStatus.NameEnum> holdItemStatus = List.of(IN_TRANSIT, CHECKED_OUT, PAGED, AWAITING_PICKUP, AWAITING_DELIVERY);
  public static final int DEFAULT_SERVICE_POINT_PERIOD_DURATION = 10;
  public static final IntervalIdEnum DEFAULT_SERVICE_POINT_PERIOD_INTERVAL = IntervalIdEnum.DAYS;
  public static final HoldShelfExpiryPeriod DEFAULT_PERIOD = HoldShelfExpiryPeriod.builder()
    .duration(DEFAULT_SERVICE_POINT_PERIOD_DURATION)
    .intervalId(DEFAULT_SERVICE_POINT_PERIOD_INTERVAL)
    .build();
  public static final String OPEN_LOAN_STATUS = "Open";
  public static final String CLOSED_LOAN_STATUS = "Closed";

}
