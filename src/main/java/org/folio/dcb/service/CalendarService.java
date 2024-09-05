package org.folio.dcb.service;

import org.folio.dcb.domain.dto.Calendar;

import java.util.UUID;

public interface CalendarService {
  Calendar findCalendarByName(String name);

  Calendar createCalendar(Calendar calendar);

  void findAndAddServicePointIdToCalendar(String calendarName, UUID servicePointIdList);

}
