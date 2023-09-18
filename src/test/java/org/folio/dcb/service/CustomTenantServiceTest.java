package org.folio.dcb.service;

import org.folio.dcb.config.props.TestTenant;
import org.folio.dcb.service.impl.CustomTenantService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomTenantServiceTest {

  private static final String TEST_TENANT = "test_tenant";
  private static final String TENANT = "tenant";
  private static final String TENANT_SCHEMA = "db_tenant";

  @Mock
  private PrepareSystemUserService systemUserService;
  @Mock
  private FolioExecutionContext context;
  @Mock
  private TestTenant testTenant;
  @Mock
  private FolioModuleMetadata moduleMetadata;
  @InjectMocks
  private CustomTenantService service;
  @Test
  void shouldPrepareSystemUser() {
    mockTenantName(TENANT);
//    mockTenantSchemaName();

    service.createOrUpdateTenant(new TenantAttributes());

    verify(systemUserService).setupSystemUser();
  }

  private void mockTenantName(String name) {
    when(context.getTenantId()).thenReturn(name);
    when(testTenant.getTenantName()).thenReturn(TEST_TENANT);
  }

  private void mockTenantSchemaName() {
    when(context.getFolioModuleMetadata()).thenReturn(moduleMetadata);
    when(moduleMetadata.getDBSchemaName(any())).thenReturn(TENANT_SCHEMA);
  }
}
