package org.folio.dcb.integration.calendar;

import org.folio.dcb.domain.dto.Calendar;
import org.folio.dcb.domain.dto.CalendarCollection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.UUID;

@HttpExchange("calendar")
public interface CalendarClient {

  @PostExchange("/calendars")
  Calendar createCalendar(@RequestBody Calendar calendar);

  @PutExchange("/calendars/{calendarId}")
  void updateCalendar(@PathVariable UUID calendarId, @RequestBody Calendar calendar);

  @GetExchange("/calendars")
  CalendarCollection getAllCalendars(@RequestParam("limit") Integer limit);
}
