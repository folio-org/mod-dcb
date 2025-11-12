package org.folio.dcb.service.impl;

import lombok.extern.log4j.Log4j2;
import org.folio.dcb.listener.kafka.service.KafkaService;
import org.folio.dcb.service.DcbHubLocationService;
import org.folio.dcb.service.entities.DcbEntityServiceFacade;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@Primary
@Lazy
public class CustomTenantService extends TenantService {

  private final KafkaService kafkaService;
  private final DcbHubLocationService dcbHubLocationService;
  private final DcbEntityServiceFacade dcbEntityServiceFacade;
  private final PrepareSystemUserService prepareSystemUserService;

  public CustomTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context, FolioSpringLiquibase folioSpringLiquibase,
                             PrepareSystemUserService prepareSystemUserService, KafkaService kafkaService,
                             DcbEntityServiceFacade dcbEntityServiceFacade, DcbHubLocationService dcbHubLocationService) {
    super(jdbcTemplate, context, folioSpringLiquibase);

    this.prepareSystemUserService = prepareSystemUserService;
    this.kafkaService = kafkaService;
    this.dcbEntityServiceFacade = dcbEntityServiceFacade;
    this.dcbHubLocationService = dcbHubLocationService;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    log.debug("afterTenantUpdate:: parameters tenantAttributes: {}", tenantAttributes);
    prepareSystemUserService.setupSystemUser();
    kafkaService.restartEventListeners();
    dcbEntityServiceFacade.createAll();
    createShadowLocations();
  }

  private void createShadowLocations() {
    try {
      dcbHubLocationService.createShadowLocations(true);
    } catch (Exception e) {
      log.error("createShadowLocations:: Error creating shadow locations: {}", e.getMessage(), e);
    }
  }
}
