package org.folio.dcb.service.impl;

import feign.FeignException;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.CancellationReasonClient;
import org.folio.dcb.client.feign.HoldingSourcesClient;
import org.folio.dcb.client.feign.HoldingsStorageClient;
import org.folio.dcb.client.feign.InstanceClient;
import org.folio.dcb.client.feign.InstanceTypeClient;
import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.client.feign.LoanTypeClient;
import org.folio.dcb.client.feign.LocationUnitClient;
import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.listener.kafka.service.KafkaService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;

import static org.folio.dcb.service.impl.ServicePointServiceImpl.HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT;
import static org.folio.dcb.utils.DCBConstants.CAMPUS_ID;
import static org.folio.dcb.utils.DCBConstants.CANCELLATION_REASON_ID;
import static org.folio.dcb.utils.DCBConstants.CODE;
import static org.folio.dcb.utils.DCBConstants.HOLDING_ID;
import static org.folio.dcb.utils.DCBConstants.INSTANCE_ID;
import static org.folio.dcb.utils.DCBConstants.INSTANCE_TITLE;
import static org.folio.dcb.utils.DCBConstants.INSTANCE_TYPE_ID;
import static org.folio.dcb.utils.DCBConstants.INSTANCE_TYPE_SOURCE;
import static org.folio.dcb.utils.DCBConstants.INSTITUTION_ID;
import static org.folio.dcb.utils.DCBConstants.LIBRARY_ID;
import static org.folio.dcb.utils.DCBConstants.LOCATION_ID;
import static org.folio.dcb.utils.DCBConstants.NAME;
import static org.folio.dcb.utils.DCBConstants.SERVICE_POINT_ID;
import static org.folio.dcb.utils.DCBConstants.SOURCE;
import static org.folio.dcb.utils.DCBConstants.LOAN_TYPE_ID;
import static org.folio.dcb.utils.DCBConstants.DCB_LOAN_TYPE_NAME;
@Log4j2
@Service
@Primary
@Lazy
public class CustomTenantService extends TenantService {

  private final PrepareSystemUserService systemUserService;
  private final KafkaService kafkaService;
  private final InstanceClient inventoryClient;
  private final InstanceTypeClient instanceTypeClient;
  private final HoldingsStorageClient holdingsStorageClient;
  private final LocationsClient locationsClient;
  private final HoldingSourcesClient holdingSourcesClient;
  private final InventoryServicePointClient servicePointClient;
  private final LocationUnitClient locationUnitClient;
  private final CancellationReasonClient cancellationReasonClient;
  private final LoanTypeClient loanTypeClient;


  public CustomTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context,
                             FolioSpringLiquibase folioSpringLiquibase, PrepareSystemUserService systemUserService, KafkaService kafkaService, InstanceClient inventoryClient, InstanceTypeClient instanceTypeClient, HoldingsStorageClient holdingsStorageClient, LocationsClient locationsClient, HoldingSourcesClient holdingSourcesClient, InventoryServicePointClient servicePointClient, LocationUnitClient locationUnitClient, LoanTypeClient loanTypeClient, CancellationReasonClient cancellationReasonClient) {
    super(jdbcTemplate, context, folioSpringLiquibase);

    this.systemUserService = systemUserService;
    this.kafkaService = kafkaService;
    this.inventoryClient = inventoryClient;
    this.instanceTypeClient = instanceTypeClient;
    this.holdingsStorageClient = holdingsStorageClient;
    this.locationsClient = locationsClient;
    this.holdingSourcesClient = holdingSourcesClient;
    this.servicePointClient = servicePointClient;
    this.locationUnitClient = locationUnitClient;
    this.loanTypeClient = loanTypeClient;
    this.cancellationReasonClient = cancellationReasonClient;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    log.debug("afterTenantUpdate:: parameters tenantAttributes: {}", tenantAttributes);
    systemUserService.setupSystemUser();
    kafkaService.restartEventListeners();
    createInstanceType();
    createInstance();
    createInstitution();
    createCampus();
    createLibrary();
    createServicePoint();
    createLocation();
    createHolding();
    createCancellationReason();
    createLoanType();
  }

  private void createLoanType() {
    if(loanTypeClient.queryLoanTypeByName(DCB_LOAN_TYPE_NAME).getTotalRecords() == 0){
      log.debug("createLoanType:: loanType creating {}", DCB_LOAN_TYPE_NAME);
      LoanTypeClient.LoanType loanType = LoanTypeClient.LoanType.builder()
        .id(LOAN_TYPE_ID)
        .name(DCB_LOAN_TYPE_NAME)
        .build();

      loanTypeClient.createLoanType(loanType);
      log.info("createLoanType:: loanType created");
    }
  }

  private void createInstanceType() {
    if (instanceTypeClient.queryInstanceTypeByName(NAME).getTotalRecords() == 0) {
      log.debug("createInstanceType:: instanceType creating instanceType");
      InstanceTypeClient.InstanceType instanceType = InstanceTypeClient.InstanceType.builder()
        .source(INSTANCE_TYPE_SOURCE)
        .code(CODE)
        .name(NAME)
        .id(INSTANCE_TYPE_ID)
        .build();

      instanceTypeClient.createInstanceType(instanceType);
      log.info("createInstanceType:: instanceType created");
    }
  }

  private void createInstance() {
    try {
      inventoryClient.getInstanceById(INSTANCE_ID);
    } catch (FeignException.NotFound ex) {
      log.debug("createInstance:: creating instance");
      InstanceClient.InventoryInstanceDTO inventoryInstanceDTO = InstanceClient.InventoryInstanceDTO.builder()
        .id(INSTANCE_ID)
        .instanceTypeId(INSTANCE_TYPE_ID)
        .title(INSTANCE_TITLE)
        .source(SOURCE)
        .build();

      inventoryClient.createInstance(inventoryInstanceDTO);
      log.info("createInstance:: instance created");
    }
  }

  private void createInstitution() {
    try {
      locationUnitClient.getInstitutionById(INSTITUTION_ID);
    } catch (FeignException.NotFound ex) {
      log.debug("createInstitution:: creating institution");
      LocationUnitClient.LocationUnit locationUnit = LocationUnitClient.LocationUnit.builder()
        .id(INSTITUTION_ID)
        .name(NAME)
        .code(CODE)
        .build();

      locationUnitClient.createInstitution(locationUnit);
      log.info("createInstitution:: institution created");
    }
  }

  private void createCampus() {
    if (locationUnitClient.getCampusByName(NAME).getTotalRecords() == 0) {
      log.debug("createCampus:: creating campus");
      LocationUnitClient.LocationUnit locationUnit = LocationUnitClient.LocationUnit.builder()
        .institutionId(INSTITUTION_ID)
        .id(CAMPUS_ID)
        .name(NAME)
        .code(CODE)
        .build();

      locationUnitClient.createCampus(locationUnit);
      log.info("createCampus:: campus created");
    }
  }

  private void createLibrary() {
    if (locationUnitClient.getLibraryByName(NAME).getTotalRecords() == 0) {
      log.debug("createLibrary:: creating library");
      LocationUnitClient.LocationUnit locationUnit = LocationUnitClient.LocationUnit.builder()
        .campusId(CAMPUS_ID)
        .id(LIBRARY_ID)
        .name(NAME)
        .code(CODE)
        .build();

      locationUnitClient.createLibrary(locationUnit);
      log.info("createLibrary:: library created");
    }
  }

  private void createServicePoint() {
    if (servicePointClient.getServicePointByName(NAME).getTotalRecords() == 0) {
      log.debug("createServicePoint:: creating service point");
      ServicePointRequest servicePointRequest = ServicePointRequest.builder()
        .id(SERVICE_POINT_ID)
        .name(NAME)
        .code(CODE)
        .discoveryDisplayName(NAME)
        .pickupLocation(true)
        .holdShelfExpiryPeriod(HoldShelfExpiryPeriod.builder().duration(3).intervalId(HoldShelfExpiryPeriod.IntervalIdEnum.DAYS).build())
        .holdShelfClosedLibraryDateManagement(HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT)
        .build();

      servicePointClient.createServicePoint(servicePointRequest);
      log.info("createServicePoint:: service point created");
    }
  }

  private void createLocation() {
    if (locationsClient.queryLocationsByName(NAME).getTotalRecords() == 0) {
      log.info("createLocation:: creating location");
      LocationsClient.LocationDTO locationDTO = LocationsClient.LocationDTO.builder()
        .id(LOCATION_ID)
        .institutionId(INSTITUTION_ID)
        .campusId(CAMPUS_ID)
        .libraryId(LIBRARY_ID)
        .primaryServicePoint(SERVICE_POINT_ID)
        .code(CODE)
        .name(NAME)
        .servicePointIds(Collections.singletonList(SERVICE_POINT_ID))
        .build();

      locationsClient.createLocation(locationDTO);
      log.info("createLocation:: location created");
    }
  }

  private void createHolding() {
    try {
      holdingsStorageClient.findHolding(HOLDING_ID);
    } catch (FeignException.NotFound ex) {
      log.debug("createHolding:: creating holding");
      HoldingsStorageClient.Holding holding = HoldingsStorageClient.Holding.builder()
        .id(HOLDING_ID)
        .instanceId(INSTANCE_ID)
        .permanentLocationId(LOCATION_ID)
        .sourceId(holdingSourcesClient.querySourceByName(SOURCE).getId())
        .build();

      holdingsStorageClient.createHolding(holding);
      log.info("createHolding:: holding created");
    }
  }

  private void createCancellationReason(){
    try {
      cancellationReasonClient.findCancellationReason(CANCELLATION_REASON_ID);
    } catch (FeignException.NotFound ex) {
      log.debug("createCancellationReason:: creating cancellation reason");
      cancellationReasonClient.createCancellationReason(CancellationReasonClient.CancellationReason.builder()
        .id(CANCELLATION_REASON_ID)
        .description("DCB Cancelled")
        .name("DCB Cancelled").build());
      log.info("createCancellationReason:: cancellation reason created");
    }
  }
}
