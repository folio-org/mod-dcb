package org.folio.dcb.service;

import feign.FeignException;
import org.folio.dcb.client.feign.*;
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

import static org.mockito.ArgumentMatchers.any;
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
}
