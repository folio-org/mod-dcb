package org.folio.dcb.service;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

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

@ExtendWith(MockitoExtension.class)
class CustomTenantServiceTest {

  @InjectMocks private CustomTenantService service;
  @Mock private KafkaService kafkaService;
  @Mock private PrepareSystemUserService systemUserService;
  @Mock private DcbEntityServiceFacade dcbEntityServiceFacade;

  @Test
  void shouldInitTenant() {
    doNothing().when(dcbEntityServiceFacade).createAll();
    doNothing().when(kafkaService).restartEventListeners();
    doNothing().when(systemUserService).setupSystemUser();

    service.createOrUpdateTenant(new TenantAttributes());
    verify(systemUserService).setupSystemUser();
  }
}
