package org.folio.dcb.service;

import feign.FeignException;
import org.folio.dcb.client.feign.CancellationReasonClient;
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

import static org.folio.dcb.utils.DCBConstants.DCB_CALENDAR_NAME;
import static org.folio.dcb.utils.DCBConstants.DEFAULT_PERIOD;
import static org.folio.dcb.utils.EntityUtils.getCalendarCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
  private LocationsClient locationsClient;
  @Mock
  private InventoryServicePointClient servicePointClient;
  @Mock
  private LocationUnitClient locationUnitClient;
  @Mock
  private LoanTypeClient loanTypeClient;
  @Mock
  private CancellationReasonClient cancellationReasonClient;
  @Mock
  private ServicePointExpirationPeriodService servicePointExpirationPeriodService;
  @Mock
  private HoldingsService holdingsService;

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
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod()).thenReturn(DEFAULT_PERIOD);

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
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(HoldingsStorageClient.Holding.builder().build());
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod()).thenReturn(DEFAULT_PERIOD);

    service.createOrUpdateTenant(new TenantAttributes());

    verify(systemUserService).setupSystemUser();
    verify(holdingsService).fetchDcbHoldingOrCreateIfMissing();
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
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(HoldingsStorageClient.Holding.builder().build());
    when(calendarService.findCalendarByName(DCB_CALENDAR_NAME)).thenReturn(null);
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod()).thenReturn(DEFAULT_PERIOD);
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod()).thenReturn(DEFAULT_PERIOD);

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
    when(holdingsService.fetchDcbHoldingOrCreateIfMissing()).thenReturn(HoldingsStorageClient.Holding.builder().build());
    when(calendarService.findCalendarByName(DCB_CALENDAR_NAME)).thenReturn(null);
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod()).thenReturn(DEFAULT_PERIOD);

    when(calendarService.findCalendarByName(DCB_CALENDAR_NAME)).thenReturn(getCalendarCollection(DCB_CALENDAR_NAME).getCalendars().getFirst());
    service.createOrUpdateTenant(new TenantAttributes());
    verify(calendarService, never()).createCalendar(any());
  }
}
