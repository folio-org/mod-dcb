package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.Calendar;
import org.folio.dcb.domain.dto.CalendarCollection;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "calendar", configuration = FeignClientConfiguration.class)
public interface CalendarClient {
  @PostMapping("/calendars")
  Calendar createCalendar(@RequestBody Calendar calendar);

  @PutMapping("/calendars/{calendarId}")
  Calendar updateCalendar(@PathVariable("calendarId") UUID calendarId, @RequestBody Calendar calendar);

  @GetMapping("/calendars")
  CalendarCollection getAllCalendars(@RequestParam("limit") Integer limit);

}
