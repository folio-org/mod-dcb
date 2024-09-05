package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.CalendarClient;
import org.folio.dcb.domain.dto.Calendar;
import org.folio.dcb.service.CalendarService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class CalendarServiceImpl implements CalendarService {
  private final CalendarClient calendarClient;

  @Override
  public Calendar findCalendarByName(String calendarName) {
    log.debug("findCalendarByName:: Trying to find the calendar with name {}", calendarName);
    return calendarClient.getAllCalendars(Integer.MAX_VALUE)
      .getCalendars()
      .stream()
      .filter(calendar -> calendar.getName().equals(calendarName))
      .findFirst()
      .orElse(null);
  }

  @Override
  public Calendar createCalendar(Calendar calendar) {
    log.debug("createCalendar:: Creating new calendar with details {}", calendar);
    return calendarClient.createCalendar(calendar);
  }

  @Override
  public void findAndAddServicePointIdToCalendar(String calendarName, UUID servicePointId) {
    log.debug("updateCalendarWithServicePointList:: find calendar by name {} to update servicePointId {}",
      calendarName, servicePointId);
    var calendar = findCalendarByName(calendarName);
    if (calendar != null) {
      calendar.getAssignments().add(servicePointId);
      calendarClient.updateCalendar(calendar.getId(), calendar);
    } else {
      log.warn("findAndAddServicePointIdToCalendar:: Calendar with name {} is not found", calendarName);
      throw new IllegalArgumentException("Calendar with name " + calendarName + " is not found");
    }
  }

}
