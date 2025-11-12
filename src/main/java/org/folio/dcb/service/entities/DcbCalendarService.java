package org.folio.dcb.service.entities;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.folio.dcb.utils.DCBConstants.DCB_CALENDAR_NAME;
import static org.folio.dcb.utils.DCBConstants.SERVICE_POINT_ID;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.Calendar;
import org.folio.dcb.domain.dto.NormalHours;
import org.folio.dcb.service.CalendarService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DcbCalendarService implements DcbEntityService<Calendar> {

  private final CalendarService calendarService;
  private final DcbServicePointService dcbServicePointService;

  @Override
  public Optional<Calendar> findDcbEntity() {
    return Optional.ofNullable(calendarService.findCalendarByName(DCB_CALENDAR_NAME));
  }

  @Override
  public Calendar createDcbEntity() {
    var servicePoint = dcbServicePointService.findOrCreateEntity();
    log.debug("createDcbEntity:: Creating a new  DCB Calendar");
    var newCalendar = getDcbCalendar(servicePoint.getId());
    var createdCalendar = calendarService.createCalendar(newCalendar);
    log.info("createDcbEntity:: DCB Calendar created");
    return createdCalendar;
  }

  @Override
  public Calendar getDefaultValue() {
    return getDcbCalendar(SERVICE_POINT_ID);
  }

  private static Calendar getDcbCalendar(String servicePointId) {
    return Calendar.builder()
      .name(DCB_CALENDAR_NAME)
      .startDate(LocalDate.now().toString())
      .endDate(LocalDate.now().plusYears(10).toString())
      .normalHours(List.of(getNormalHours()))
      .assignments(List.of(UUID.fromString(defaultIfNull(servicePointId, SERVICE_POINT_ID))))
      .exceptions(emptyList())
      .build();
  }

  private static NormalHours getNormalHours() {
    return NormalHours.builder()
      .startDay(DayOfWeek.SUNDAY.name())
      .startTime(LocalTime.of(0, 0).toString())
      .endDay(DayOfWeek.SATURDAY.toString())
      .endTime(LocalTime.of(23, 59).toString())
      .build();
  }
}
