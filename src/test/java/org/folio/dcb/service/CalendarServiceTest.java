package org.folio.dcb.service;

import org.folio.dcb.client.feign.CalendarClient;
import org.folio.dcb.domain.dto.Calendar;
import org.folio.dcb.service.impl.CalendarServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.folio.dcb.utils.DCBConstants.SERVICE_POINT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.folio.dcb.utils.DCBConstants.DCB_CALENDAR_NAME;
import static org.folio.dcb.utils.EntityUtils.getCalendarCollection;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {
  @InjectMocks
  private CalendarServiceImpl calendarService;

  @Mock
  private CalendarClient calendarClient;

  @Test
  void testCreateCalendar() {
    var calendar = new Calendar();
    calendarService.createCalendar(calendar);
    verify(calendarClient).createCalendar(calendar);
  }

  @Test
  void testFindCalendarByName() {
    when(calendarClient.getAllCalendars(Integer.MAX_VALUE))
      .thenReturn(getCalendarCollection(DCB_CALENDAR_NAME));
    var response = calendarService.findCalendarByName(DCB_CALENDAR_NAME);
    verify(calendarClient).getAllCalendars(Integer.MAX_VALUE);
    assertNotNull(response);
    assertEquals(DCB_CALENDAR_NAME, response.getName());
  }

  @Test
  void testFindCalendar_InvalidName() {
    when(calendarClient.getAllCalendars(Integer.MAX_VALUE))
      .thenReturn(getCalendarCollection(DCB_CALENDAR_NAME));
    var response = calendarService.findCalendarByName("");
    verify(calendarClient).getAllCalendars(Integer.MAX_VALUE);
    assertNull(response);
  }

  @Test
  void testAddServicePointIdToDefaultCalendar() {
    when(calendarClient.getAllCalendars(Integer.MAX_VALUE))
      .thenReturn(getCalendarCollection(DCB_CALENDAR_NAME));
    calendarService.addServicePointIdToDefaultCalendar(UUID.fromString(SERVICE_POINT_ID));
    verify(calendarClient).getAllCalendars(Integer.MAX_VALUE);
    verify(calendarClient).updateCalendar(any(), any());
  }

  @Test
  void testAddServicePointIdToDefaultCalendar_DefaultCalendarNotExists() {
    when(calendarClient.getAllCalendars(Integer.MAX_VALUE))
      .thenReturn(getCalendarCollection("test"));
    var servicePointId = UUID.randomUUID();
    assertThrows(IllegalArgumentException.class,
      () -> calendarService.addServicePointIdToDefaultCalendar(servicePointId));
  }

  @Test
  void testAssociateServicePointIdWithDefaultCalendar_SpNotAssociated() {
    when(calendarClient.getAllCalendars(Integer.MAX_VALUE))
      .thenReturn(getCalendarCollection(DCB_CALENDAR_NAME));
    var servicePointId = UUID.randomUUID();
    calendarService.associateServicePointIdWithDefaultCalendarIfAbsent(servicePointId);
    verify(calendarClient).getAllCalendars(Integer.MAX_VALUE);
    verify(calendarClient).updateCalendar(any(), any());
  }

  @Test
  void testAssociateServicePointIdWithDefaultCalendar_SpAssociated() {
    var servicePointId = UUID.randomUUID();
    var calendarCollection = getCalendarCollection(DCB_CALENDAR_NAME);
    // Adding the sp in assignments to make sure that our code will not try to update the calendar again
    calendarCollection.getCalendars().get(0).getAssignments().add(servicePointId);
    when(calendarClient.getAllCalendars(Integer.MAX_VALUE))
      .thenReturn(calendarCollection);
    calendarService.associateServicePointIdWithDefaultCalendarIfAbsent(servicePointId);
    verify(calendarClient).getAllCalendars(Integer.MAX_VALUE);
    verify(calendarClient, never()).updateCalendar(any(), any());
  }

  @Test
  void testAssociateServicePointIdWithDefaultCalendar_DefaultCalendarNotExists() {
    var servicePointId = UUID.randomUUID();
    var calendarCollection = getCalendarCollection("test");
    when(calendarClient.getAllCalendars(Integer.MAX_VALUE))
      .thenReturn(calendarCollection);
    assertThrows(IllegalArgumentException.class,
      () -> calendarService.associateServicePointIdWithDefaultCalendarIfAbsent(servicePointId));
  }

}
