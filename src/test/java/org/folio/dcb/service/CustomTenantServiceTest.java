package org.folio.dcb.service;

import org.folio.dcb.domain.dto.RefreshShadowLocationResponse;
import org.folio.dcb.listener.kafka.service.KafkaService;
import org.folio.dcb.service.entities.DcbEntityServiceFacade;
import org.folio.dcb.service.impl.CustomTenantService;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomTenantServiceTest {

  @InjectMocks private CustomTenantService service;
  @Mock private KafkaService kafkaService;
  @Mock private PrepareSystemUserService systemUserService;
  @Mock private DcbHubLocationService dcbHubLocationService;
  @Mock private DcbEntityServiceFacade dcbEntityServiceFacade;

  @Test
  void shouldInitTenant() {
    doNothing().when(dcbEntityServiceFacade).createAll();;
    doNothing().when(kafkaService).restartEventListeners();
    doNothing().when(systemUserService).setupSystemUser();
    when(dcbHubLocationService.createShadowLocations(true))
      .thenReturn(new RefreshShadowLocationResponse());

    service.createOrUpdateTenant(new TenantAttributes());
    verify(systemUserService).setupSystemUser();
  }
}
