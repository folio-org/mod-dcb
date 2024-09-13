package org.folio.dcb.service;

import feign.FeignException;
import org.folio.dcb.client.feign.CancellationReasonClient;
import org.folio.dcb.client.feign.HoldingSourcesClient;
import org.folio.dcb.client.feign.HoldingsStorageClient;
import org.folio.dcb.client.feign.InstanceTypeClient;
import org.folio.dcb.client.feign.InstanceClient;
import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.client.feign.LoanTypeClient;
import org.folio.dcb.client.feign.LocationUnitClient;
import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.listener.kafka.service.KafkaService;
import org.folio.dcb.service.impl.CustomTenantService;
import org.folio.spring.model.ResultList;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.folio.dcb.utils.DCBConstants.DCB_CALENDAR_NAME;
import static org.folio.dcb.utils.EntityUtils.getCalendarCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomTenantServiceTest {

  @Mock
  private PrepareSystemUserService systemUserService;
  @Mock
  private KafkaService kafkaService;
  @Mock
  private InstanceClient inventoryClient;
  @Mock
  private InstanceTypeClient instanceTypeClient;
  @Mock
  private HoldingsStorageClient holdingsStorageClient;
  @Mock
  private LocationsClient locationsClient;
  @Mock
  private HoldingSourcesClient holdingSourcesClient;
  @Mock
  private InventoryServicePointClient servicePointClient;
  @Mock
  private LocationUnitClient locationUnitClient;
  @Mock
  private LoanTypeClient loanTypeClient;
  @Mock
  private CancellationReasonClient cancellationReasonClient;

  @Mock
  private CalendarService calendarService;

  @InjectMocks
  private CustomTenantService service;

  @Test
  void shouldPrepareSystemUser() {
    when(instanceTypeClient.queryInstanceTypeByName(any())).thenReturn(new ResultList<>());
    when(locationUnitClient.getCampusByName(any())).thenReturn(new ResultList<>());
    when(inventoryClient.getInstanceById(any())).thenThrow(FeignException.NotFound.class);
    when(locationUnitClient.getLibraryByName(any())).thenReturn(new ResultList<>());
    when(servicePointClient.getServicePointByName(any())).thenReturn(new ResultList<>());
    when(locationsClient.queryLocationsByName(any())).thenReturn(new ResultList<>());
    when(loanTypeClient.queryLoanTypeByName(any())).thenReturn(new ResultList<>());

    service.createOrUpdateTenant(new TenantAttributes());
    verify(systemUserService).setupSystemUser();
  }

  @Test
  void testHoldingsCreationWhileEnablingTenant() {
    when(instanceTypeClient.queryInstanceTypeByName(any())).thenReturn(new ResultList<>());
    when(locationUnitClient.getCampusByName(any())).thenReturn(new ResultList<>());
    when(inventoryClient.getInstanceById(any())).thenThrow(FeignException.NotFound.class);
    when(locationUnitClient.getLibraryByName(any())).thenReturn(new ResultList<>());
    when(servicePointClient.getServicePointByName(any())).thenReturn(new ResultList<>());
    when(locationsClient.queryLocationsByName(any())).thenReturn(new ResultList<>());
    when(loanTypeClient.queryLoanTypeByName(any())).thenReturn(new ResultList<>());
    when(holdingsStorageClient.findHolding(any())).thenReturn(HoldingsStorageClient.Holding.builder().build());
    service.createOrUpdateTenant(new TenantAttributes());
    verify(systemUserService).setupSystemUser();

    when(holdingsStorageClient.findHolding(any())).thenThrow(FeignException.NotFound.class);
    when(holdingSourcesClient.querySourceByName("FOLIO"))
      .thenReturn(ResultList.of(1, List.of(createHoldingRecordSource())));

    service.createOrUpdateTenant(new TenantAttributes());
    verify(systemUserService, times(2)).setupSystemUser();
    verify(holdingSourcesClient, never()).createHoldingsRecordSource(any());
    verify(holdingsStorageClient).createHolding(any());

    when(holdingSourcesClient.querySourceByName("FOLIO"))
      .thenReturn(ResultList.of(0, List.of()));
    when(holdingSourcesClient.createHoldingsRecordSource(any()))
      .thenReturn(createHoldingRecordSource());

    service.createOrUpdateTenant(new TenantAttributes());
    verify(systemUserService, times(3)).setupSystemUser();
    verify(holdingSourcesClient).createHoldingsRecordSource(any());
    verify(holdingsStorageClient, times(2)).createHolding(any());
  }

  @Test
  void testCalendarCreation_DefaultCalendarNotExists() {
    when(instanceTypeClient.queryInstanceTypeByName(any())).thenReturn(new ResultList<>());
    when(locationUnitClient.getCampusByName(any())).thenReturn(new ResultList<>());
    when(inventoryClient.getInstanceById(any())).thenThrow(FeignException.NotFound.class);
    when(locationUnitClient.getLibraryByName(any())).thenReturn(new ResultList<>());
    when(servicePointClient.getServicePointByName(any())).thenReturn(new ResultList<>());
    when(locationsClient.queryLocationsByName(any())).thenReturn(new ResultList<>());
    when(loanTypeClient.queryLoanTypeByName(any())).thenReturn(new ResultList<>());
    when(holdingsStorageClient.findHolding(any())).thenReturn(HoldingsStorageClient.Holding.builder().build());
    when(calendarService.findCalendarByName(DCB_CALENDAR_NAME)).thenReturn(null);

    service.createOrUpdateTenant(new TenantAttributes());
    verify(calendarService).createCalendar(any());
  }

  @Test
  void testCalendarCreation_DefaultCalendarExists() {
    when(instanceTypeClient.queryInstanceTypeByName(any())).thenReturn(new ResultList<>());
    when(locationUnitClient.getCampusByName(any())).thenReturn(new ResultList<>());
    when(inventoryClient.getInstanceById(any())).thenThrow(FeignException.NotFound.class);
    when(locationUnitClient.getLibraryByName(any())).thenReturn(new ResultList<>());
    when(servicePointClient.getServicePointByName(any())).thenReturn(new ResultList<>());
    when(locationsClient.queryLocationsByName(any())).thenReturn(new ResultList<>());
    when(loanTypeClient.queryLoanTypeByName(any())).thenReturn(new ResultList<>());
    when(holdingsStorageClient.findHolding(any())).thenReturn(HoldingsStorageClient.Holding.builder().build());
    when(calendarService.findCalendarByName(DCB_CALENDAR_NAME)).thenReturn(null);

    when(calendarService.findCalendarByName(DCB_CALENDAR_NAME)).thenReturn(getCalendarCollection(DCB_CALENDAR_NAME).getCalendars().get(0));
    service.createOrUpdateTenant(new TenantAttributes());
    verify(calendarService, never()).createCalendar(any());
  }

  private HoldingSourcesClient.HoldingSource createHoldingRecordSource() {
    return HoldingSourcesClient.HoldingSource
      .builder()
      .id(UUID.randomUUID().toString())
      .build();
  }
}
