package org.folio.dcb.service;

import static java.lang.Integer.MAX_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.dcb.utils.DcbConstants.DCB_CALENDAR_NAME;
import static org.folio.dcb.utils.DcbConstants.SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.getCalendarCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.dcb.domain.dto.Calendar;
import org.folio.dcb.domain.dto.CalendarCollection;
import org.folio.dcb.integration.calendar.CalendarClient;
import org.folio.dcb.service.impl.CalendarServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

  @InjectMocks private CalendarServiceImpl calendarService;
  @Mock private CalendarClient calendarClient;

  @Test
  void testCreateCalendar() {
    var calendar = new Calendar();
    calendarService.createCalendar(calendar);
    verify(calendarClient).createCalendar(calendar);
  }

  @Test
  void testFindCalendarByName() {
    when(calendarClient.getAllCalendars(MAX_VALUE)).thenReturn(getCalendarCollection(DCB_CALENDAR_NAME));
    var response = calendarService.findCalendarByName(DCB_CALENDAR_NAME);
    verify(calendarClient).getAllCalendars(MAX_VALUE);
    assertThat(response).isNotNull();
    assertThat(response.getName()).isEqualTo(DCB_CALENDAR_NAME);
  }

  @Test
  void testFindCalendar_InvalidName() {
    when(calendarClient.getAllCalendars(MAX_VALUE)).thenReturn(getCalendarCollection(DCB_CALENDAR_NAME));
    var response = calendarService.findCalendarByName("");
    verify(calendarClient).getAllCalendars(MAX_VALUE);
    assertThat(response).isNull();
  }

  @Test
  void testAddServicePointIdToDefaultCalendar() {
    when(calendarClient.getAllCalendars(MAX_VALUE)).thenReturn(getCalendarCollection(DCB_CALENDAR_NAME));
    calendarService.addServicePointIdToDefaultCalendar(UUID.fromString(SERVICE_POINT_ID));
    verify(calendarClient).getAllCalendars(MAX_VALUE);
    verify(calendarClient).updateCalendar(any(), any());
  }

  @Test
  void testAddServicePointIdToDefaultCalendar_DefaultCalendarNotExists() {
    when(calendarClient.getAllCalendars(MAX_VALUE)).thenReturn(getCalendarCollection("test"));
    var servicePointId = UUID.randomUUID();
    assertThatThrownBy(() -> calendarService.addServicePointIdToDefaultCalendar(servicePointId))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testAssociateServicePointIdWithDefaultCalendar_SpNotAssociated() {
    when(calendarClient.getAllCalendars(MAX_VALUE))
      .thenReturn(getCalendarCollection(DCB_CALENDAR_NAME));
    var servicePointId = UUID.randomUUID();

    calendarService.associateServicePointIdWithDefaultCalendarIfAbsent(servicePointId);

    verify(calendarClient).getAllCalendars(MAX_VALUE);
    verify(calendarClient).updateCalendar(any(), any());
  }

  @Test
  void testAssociateServicePointIdWithDefaultCalendar_SpAssociated() {
    var servicePointId = UUID.randomUUID();
    var calendarCollection = getCalendarCollection(DCB_CALENDAR_NAME);
    // Adding the sp in assignments to make sure that our code will not try to update the calendar again
    calendarCollection.getCalendars().get(0).getAssignments().add(servicePointId);
    when(calendarClient.getAllCalendars(MAX_VALUE)).thenReturn(calendarCollection);

    calendarService.associateServicePointIdWithDefaultCalendarIfAbsent(servicePointId);

    verify(calendarClient).getAllCalendars(MAX_VALUE);
    verify(calendarClient, never()).updateCalendar(any(), any());
  }

  @Test
  void testAssociateServicePointIdWithDefaultCalendar_DefaultCalendarNotExists() {
    var servicePointId = UUID.randomUUID();
    var calendarCollection = getCalendarCollection("test");
    when(calendarClient.getAllCalendars(MAX_VALUE)).thenReturn(calendarCollection);
    assertThatThrownBy(() -> calendarService.associateServicePointIdWithDefaultCalendarIfAbsent(servicePointId))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void findCalendarByName_positive_emptyCollection() {
    // TestMate-38edfab7aa6fbd359b174576a85ed904
    var calendarCollection = new CalendarCollection().totalRecords(0);
    when(calendarClient.getAllCalendars(MAX_VALUE)).thenReturn(calendarCollection);

    var response = calendarService.findCalendarByName(DCB_CALENDAR_NAME);

    verify(calendarClient).getAllCalendars(MAX_VALUE);
    assertThat(response).isNull();
  }

  @Test
  void findCalendarByName_positive_exactMatch() {
    // TestMate-4b2bf27890ab20d8e60fc02676886bc0
    var calendarName = "Main Calendar";
    when(calendarClient.getAllCalendars(MAX_VALUE)).thenReturn(getCalendarCollection(calendarName));

    var inputName = "Main Calendar";
    var response = calendarService.findCalendarByName(inputName);

    verify(calendarClient).getAllCalendars(MAX_VALUE);
    assertThat(response).isNotNull();
    assertThat(calendarName).isEqualTo(response.getName());
  }

  @ParameterizedTest
  @CsvSource({
    "Main",
    "main calendar",
    "'Main Calendar '"
  })
  void findCalendarByName_positive_notMatched(String inputName) {
    var calendarName = "Main Calendar";
    when(calendarClient.getAllCalendars(MAX_VALUE)).thenReturn(getCalendarCollection(calendarName));

    var response = calendarService.findCalendarByName(inputName);

    verify(calendarClient).getAllCalendars(MAX_VALUE);
    assertThat(response).isNull();
  }

    @Test
  void testAssociateServicePointIdWithDefaultCalendar_SpAssociatedWithDifferentCalendar() {
    // TestMate-3a1f695716ffc943fd635aeb5ea9da1f
    // Given
    var servicePointId = UUID.fromString("00000000-1111-2222-3333-444444444444");
    var otherCalendar = Calendar.builder()
      .name("Other-Calendar")
      .assignments(new ArrayList<>(List.of(servicePointId)))
      .build();
    var defaultCalendar = Calendar.builder()
      .name(DCB_CALENDAR_NAME)
      .assignments(new ArrayList<>())
      .build();
    var calendarCollection = new CalendarCollection()
      .calendars(List.of(otherCalendar, defaultCalendar))
      .totalRecords(2);
    when(calendarClient.getAllCalendars(MAX_VALUE)).thenReturn(calendarCollection);
    // When
    calendarService.associateServicePointIdWithDefaultCalendarIfAbsent(servicePointId);
    // Then
    verify(calendarClient).getAllCalendars(MAX_VALUE);
    verify(calendarClient, never()).updateCalendar(any(), any());
  }
}
