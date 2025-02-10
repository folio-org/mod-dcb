package org.folio.dcb.service;

import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.service.impl.ServicePointServiceImpl;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.folio.dcb.utils.EntityUtils.createDcbPickup;
import static org.folio.dcb.utils.EntityUtils.createServicePointRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.IntervalIdEnum;
import org.folio.dcb.service.impl.ServicePointServiceImpl;
import org.folio.dcb.utils.DCBConstants;

@ExtendWith(MockitoExtension.class)
class ServicePointServiceTest {

  @InjectMocks
  private ServicePointServiceImpl servicePointService;

  @Mock
  private InventoryServicePointClient inventoryServicePointClient;

  @Mock
  private CalendarService calendarService;

  @Mock private static ServicePointExpirationPeriodService servicePointExpirationPeriodService;

  @Test
  void createServicePointIfNotExistsTest(){
    when(inventoryServicePointClient.getServicePointByName(any()))
      .thenReturn(ResultList.of(0, List.of()));
    when(inventoryServicePointClient.createServicePoint(any()))
      .thenReturn(createServicePointRequest());
    var response = servicePointService.createServicePointIfNotExists(createDcbPickup());
    verify(inventoryServicePointClient).createServicePoint(any());
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod()).thenReturn(DCBConstants.DEFAULT_PERIOD);
    verify(inventoryServicePointClient).getServicePointByName(any());
    verify(calendarService).addServicePointIdToDefaultCalendar(UUID.fromString(response.getId()));
    verify(calendarService, never()).associateServicePointIdWithDefaultCalendarIfAbsent(any());
  }

  @Test
  void createServicePointIfExistsTest(){
    var servicePointRequest = createServicePointRequest();
    var servicePointId = UUID.randomUUID().toString();
    servicePointRequest.setId(servicePointId);
    when(inventoryServicePointClient.getServicePointByName(any()))
      .thenReturn(ResultList.of(0, List.of(servicePointRequest)));
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod()).thenReturn(
      HoldShelfExpiryPeriod.builder()
        .duration(2)
        .intervalId(IntervalIdEnum.MONTHS)
        .build()
    );
    var response = servicePointService.createServicePointIfNotExists(createDcbPickup());
    assertEquals(servicePointId, response.getId());
    assertEquals(2, response.getHoldShelfExpiryPeriod().getDuration());
    assertEquals(IntervalIdEnum.MONTHS, response.getHoldShelfExpiryPeriod().getIntervalId());
    verify(inventoryServicePointClient, times(1)).updateServicePointById(any(), any());
    verify(inventoryServicePointClient, never()).createServicePoint(any());
    verify(inventoryServicePointClient).getServicePointByName(any());
    verify(calendarService).associateServicePointIdWithDefaultCalendarIfAbsent(
      UUID.fromString(response.getId()));
    verify(calendarService).associateServicePointIdWithDefaultCalendarIfAbsent(UUID.fromString(response.getId()));
    verify(calendarService, never()).addServicePointIdToDefaultCalendar(any());
  }

}
