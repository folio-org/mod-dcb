package org.folio.dcb.service;

import org.folio.dcb.domain.dto.Calendar;

import java.util.UUID;

public interface CalendarService {
  /**
   * Finds a calendar by its name.
   *
   * @param name the name of the calendar to find.
   * @return the {@link Calendar} object with the specified name,
   * or {@code null} if no such calendar is found.
   */
  Calendar findCalendarByName(String name);

  /**
   * Creates a new calendar.
   *
   * @param calendar the {@link Calendar} object to be created.
   * @return the created {@link Calendar} object.
   */
  Calendar createCalendar(Calendar calendar);

  /**
   * Adds a service point ID with a default calendar.
   * If the calendar is found, the service point ID is added to the calendar's assignments.
   *
   * @param servicePointId the unique identifier of the service point to add.
   * @throws IllegalArgumentException if the default calendar name is not found.
   */
  void addServicePointIdToDefaultCalendar(UUID servicePointId);

  /**
   * Associates a service point ID with a default calendar if it is not already associated
   * with any calendar. Checks existing calendars and adds the service point ID to the
   * default calendar if absent.
   *
   * @param servicePointId the unique identifier of the service point to associate.
   * @throws IllegalArgumentException if the default calendar name is not found.
   */
  void associateServicePointIdWithDefaultCalendarIfAbsent(UUID servicePointId);

}
