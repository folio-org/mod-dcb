package org.folio.dcb.service;

import org.folio.dcb.service.impl.CustomTenantService;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CustomTenantServiceTest {

  @Mock
  private PrepareSystemUserService systemUserService;
  @InjectMocks
  private CustomTenantService service;
  @Test
  void shouldPrepareSystemUser() {
    service.createOrUpdateTenant(new TenantAttributes());
    verify(systemUserService).setupSystemUser();
  }
}
