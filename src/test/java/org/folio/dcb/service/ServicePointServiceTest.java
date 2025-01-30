package org.folio.dcb.service;

import static org.folio.dcb.utils.EntityUtils.createDcbPickup;
import static org.folio.dcb.utils.EntityUtils.createServicePointRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.IntervalIdEnum;
import org.folio.dcb.domain.entity.ServicePointExpirationPeriodEntity;
import org.folio.dcb.repository.ServicePointExpirationPeriodRepository;
import org.folio.dcb.service.impl.ServicePointServiceImpl;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
class ServicePointServiceTest {

  @InjectMocks
  private ServicePointServiceImpl servicePointService;

  @Mock
  private InventoryServicePointClient inventoryServicePointClient;

  @Mock
  private CalendarService calendarService;

  @Mock
  private ServicePointExpirationPeriodRepository servicePointExpirationPeriodRepository;

  @Test
  @SneakyThrows
  void shouldSetDefaultHoldShelfPeriodIfTableIsEmpty() {
    when(servicePointExpirationPeriodRepository.findAll()).thenReturn(List.of());
    Method method = servicePointService.getClass()
      .getDeclaredMethod("getShelfExpiryPeriod");
    method.setAccessible(true);
    HoldShelfExpiryPeriod result =  (HoldShelfExpiryPeriod) method.invoke(servicePointService);
    assertEquals(10, result.getDuration());
    assertEquals(IntervalIdEnum.DAYS, result.getIntervalId());
  }
  @Test
  @SneakyThrows
  void shouldSetCustomHoldShelfPeriodRelatedToValueFromTable() {
    when(servicePointExpirationPeriodRepository.findAll()).thenReturn(List.of(
      ServicePointExpirationPeriodEntity.builder()
        .duration(3)
        .intervalId(IntervalIdEnum.MINUTES)
        .build()
    ));
    Method method = servicePointService.getClass()
      .getDeclaredMethod("getShelfExpiryPeriod");
    method.setAccessible(true);
    HoldShelfExpiryPeriod result =  (HoldShelfExpiryPeriod) method.invoke(servicePointService);
    assertEquals(3, result.getDuration());
    assertEquals(IntervalIdEnum.MINUTES, result.getIntervalId());
  }


  @Test
  void createServicePointIfNotExistsTest() {
    when(inventoryServicePointClient.getServicePointByName(any()))
      .thenReturn(ResultList.of(0, List.of()));
    when(inventoryServicePointClient.createServicePoint(any()))
      .thenReturn(createServicePointRequest());
    when(servicePointExpirationPeriodRepository.findAll()).thenReturn(Collections.emptyList());
    var response = servicePointService.createServicePointIfNotExists(createDcbPickup());
    verify(inventoryServicePointClient).createServicePoint(any());
    verify(inventoryServicePointClient).getServicePointByName(any());
    verify(calendarService).addServicePointIdToDefaultCalendar(UUID.fromString(response.getId()));
    verify(calendarService, never()).associateServicePointIdWithDefaultCalendarIfAbsent(any());
  }

  @Test
  void createServicePointIfExistsTest() {
    var servicePointRequest = createServicePointRequest();
    var servicePointId = UUID.randomUUID().toString();
    servicePointRequest.setId(servicePointId);
    when(inventoryServicePointClient.getServicePointByName(any()))
      .thenReturn(ResultList.of(0, List.of(servicePointRequest)));
    var response = servicePointService.createServicePointIfNotExists(createDcbPickup());
    assertEquals(servicePointId, response.getId());
//    verify(inventoryServicePointClient, never()).createServicePoint(any());
    verify(inventoryServicePointClient).getServicePointByName(any());
    verify(calendarService).associateServicePointIdWithDefaultCalendarIfAbsent(UUID.fromString(response.getId()));
    verify(calendarService, never()).addServicePointIdToDefaultCalendar(any());
  }

}
