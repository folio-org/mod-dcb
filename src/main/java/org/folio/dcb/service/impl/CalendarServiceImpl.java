package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.CalendarClient;
import org.folio.dcb.domain.dto.Calendar;
import org.folio.dcb.service.CalendarService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static org.folio.dcb.utils.DCBConstants.DCB_CALENDAR_NAME;

@Service
@RequiredArgsConstructor
@Log4j2
public class CalendarServiceImpl implements CalendarService {
  private final CalendarClient calendarClient;

  @Override
  public Calendar findCalendarByName(String calendarName) {
    log.debug("findCalendarByName:: Trying to find the calendar with name {}", calendarName);
    var calendars = getAllCalendars();
    return findCalendarByName(calendars, calendarName);
  }

  @Override
  public Calendar createCalendar(Calendar calendar) {
    log.debug("createCalendar:: Creating new calendar with details {}", calendar);
    return calendarClient.createCalendar(calendar);
  }

  @Override
  public void addServicePointIdToDefaultCalendar(UUID servicePointId) {
    log.debug("updateCalendarWithServicePointList:: find calendar by name {} to update servicePointId {}",
      DCB_CALENDAR_NAME, servicePointId);
    var calendar = findCalendarByName(DCB_CALENDAR_NAME);
    updateCalendarIfExists(DCB_CALENDAR_NAME, servicePointId, calendar);
  }

  private void updateCalendarIfExists(String calendarName, UUID servicePointId, Calendar calendar) {
    if (calendar != null) {
      calendar.getAssignments().add(servicePointId);
      calendarClient.updateCalendar(calendar.getId(), calendar);
    } else {
      log.warn("findAndAddServicePointIdToCalendar:: Calendar with name {} is not found", calendarName);
      throw new IllegalArgumentException("Calendar with name " + calendarName + " is not found");
    }
  }

  @Override
  public void associateServicePointIdWithDefaultCalendarIfAbsent(UUID servicePointId) {
    var calendars = getAllCalendars();
    if (checkServicePointIdAssociatedWithAnyCalendar(calendars, servicePointId)) {
      log.info("associateServicePointIdWithDefaultCalendarIfAbsent:: servicePointId {} is already " +
        "associated with calendar", servicePointId);
    } else {
      log.info("associateServicePointIdWithDefaultCalendarIfAbsent:: servicePointId {} is not " +
        "associated with any calendar. so associating with default calendar", servicePointId);
      var defaultDcbCalendar = findCalendarByName(calendars, DCB_CALENDAR_NAME);
      updateCalendarIfExists(DCB_CALENDAR_NAME, servicePointId, defaultDcbCalendar);
    }
  }

  private List<Calendar> getAllCalendars() {
    log.debug("getAllCalendars:: Fetching all calendars");
    return calendarClient.getAllCalendars(Integer.MAX_VALUE)
      .getCalendars();
  }

  private Calendar findCalendarByName(List<Calendar> calendars, String calendarName) {
    log.debug("findCalendarByName:: Finding calendar with name {} from calendarList {}", calendarName, calendars);
    return calendars
      .stream()
      .filter(calendar -> calendar.getName().equals(calendarName))
      .findFirst()
      .orElse(null);
  }

  private boolean checkServicePointIdAssociatedWithAnyCalendar(List<Calendar> calendars, UUID servicePointId) {
    log.debug("checkServicePointIdAssociatedWithAnyCalendar:: checking servicePointId {} associated with " +
      "any calendar {}", servicePointId, calendars);
    return calendars.stream()
      .anyMatch(calendar -> calendar.getAssignments().contains(servicePointId));
  }

}
