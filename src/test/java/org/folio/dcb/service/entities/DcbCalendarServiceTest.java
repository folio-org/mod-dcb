package org.folio.dcb.service.entities;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.folio.dcb.domain.dto.Calendar;
import org.folio.dcb.domain.dto.NormalHours;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.service.CalendarService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DcbCalendarServiceTest {

  private static final String DCB_CALENDAR_NAME = "DCB Calendar";
  private static final String SERVICE_POINT_ID = "9d1b77e8-f02e-4b7f-b296-3f2042ddac54";

  @InjectMocks private DcbCalendarService dcbCalendarService;
  @Mock private CalendarService calendarService;
  @Mock private DcbServicePointService dcbServicePointService;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(calendarService, dcbServicePointService);
  }

  @Test
  void findDcbEntity_positive_shouldReturnCalendarWhenExists() {
    var expectedCalendar = dcbCalendar();
    when(calendarService.findCalendarByName(DCB_CALENDAR_NAME)).thenReturn(expectedCalendar);
    var result = dcbCalendarService.findDcbEntity();
    assertThat(result).contains(dcbCalendar());
  }

  @Test
  void findDcbEntity_positive_shouldReturnEmptyWhenNotExists() {
    when(calendarService.findCalendarByName(DCB_CALENDAR_NAME)).thenReturn(null);
    var result = dcbCalendarService.findDcbEntity();
    assertThat(result).isEmpty();
  }

  @Test
  void createDcbEntity_positive_shouldCreateCalendarWithServicePoint() {
    var servicePoint = dcbServicePoint();
    var expectedCalendar = dcbCalendar();
    when(dcbServicePointService.findOrCreateEntity()).thenReturn(servicePoint);
    when(calendarService.createCalendar(expectedCalendar)).thenReturn(expectedCalendar);

    var result = dcbCalendarService.createDcbEntity();

    assertThat(result).isEqualTo(dcbCalendar());
  }

  @Test
  void getDefaultValue_positive_shouldReturnCalendarWithDefaultValues() {
    var result = dcbCalendarService.getDefaultValue();
    assertThat(result).isEqualTo(dcbCalendar());
  }

  @Test
  void findOrCreateEntity_positive_shouldReturnExistingCalendar() {
    var expectedCalendar = dcbCalendar();
    when(calendarService.findCalendarByName(DCB_CALENDAR_NAME)).thenReturn(expectedCalendar);

    var result = dcbCalendarService.findOrCreateEntity();

    assertThat(result).isEqualTo(dcbCalendar());
    verify(calendarService, never()).createCalendar(expectedCalendar);
    verify(dcbServicePointService, never()).findOrCreateEntity();
  }

  @Test
  void findOrCreateEntity_positive_shouldCreateCalendarWhenNotExists() {
    var servicePoint = dcbServicePoint();
    var expectedCalendar = dcbCalendar();

    when(calendarService.findCalendarByName(DCB_CALENDAR_NAME)).thenReturn(null);
    when(dcbServicePointService.findOrCreateEntity()).thenReturn(servicePoint);
    when(calendarService.createCalendar(expectedCalendar)).thenReturn(expectedCalendar);

    var result = dcbCalendarService.findOrCreateEntity();

    assertThat(result).isEqualTo(dcbCalendar());
  }

  private static Calendar dcbCalendar() {
    return Calendar.builder()
      .name(DCB_CALENDAR_NAME)
      .startDate(LocalDate.now().toString())
      .endDate(LocalDate.now().plusYears(10).toString())
      .normalHours(List.of(getNormalHours()))
      .assignments(List.of(UUID.fromString(SERVICE_POINT_ID)))
      .exceptions(List.of())
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

  private static ServicePointRequest dcbServicePoint() {
    return ServicePointRequest.builder()
      .id(SERVICE_POINT_ID)
      .build();
  }
}
