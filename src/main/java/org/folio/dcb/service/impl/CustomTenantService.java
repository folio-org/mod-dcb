package org.folio.dcb.service.impl;

import feign.FeignException;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.CancellationReasonClient;
import org.folio.dcb.client.feign.InstanceClient;
import org.folio.dcb.client.feign.InstanceTypeClient;
import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.client.feign.LoanTypeClient;
import org.folio.dcb.client.feign.LocationUnitClient;
import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.domain.dto.Calendar;
import org.folio.dcb.domain.dto.NormalHours;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.listener.kafka.service.KafkaService;
import org.folio.dcb.service.CalendarService;
import org.folio.dcb.service.HoldingsService;
import org.folio.dcb.service.ServicePointExpirationPeriodService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.folio.dcb.service.impl.ServicePointServiceImpl.HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT;
import static org.folio.dcb.utils.DCBConstants.CAMPUS_ID;
import static org.folio.dcb.utils.DCBConstants.CANCELLATION_REASON_ID;
import static org.folio.dcb.utils.DCBConstants.CODE;
import static org.folio.dcb.utils.DCBConstants.DCB_CALENDAR_NAME;
import static org.folio.dcb.utils.DCBConstants.DCB_CANCELLATION_REASON_NAME;
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

  private final PrepareSystemUserService prepareSystemUserService;
  private final KafkaService kafkaService;
  private final InstanceClient inventoryClient;
  private final InstanceTypeClient instanceTypeClient;
  private final LocationsClient locationsClient;
  private final InventoryServicePointClient servicePointClient;
  private final LocationUnitClient locationUnitClient;
  private final CancellationReasonClient cancellationReasonClient;
  private final LoanTypeClient loanTypeClient;
  private final CalendarService calendarService;
  private final ServicePointExpirationPeriodService servicePointExpirationPeriodService;
  private final HoldingsService holdingsService;


  public CustomTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context, FolioSpringLiquibase folioSpringLiquibase,
                             PrepareSystemUserService prepareSystemUserService, KafkaService kafkaService, InstanceClient inventoryClient,
                             InstanceTypeClient instanceTypeClient, LocationsClient locationsClient,
                             InventoryServicePointClient servicePointClient, LocationUnitClient locationUnitClient,
                             LoanTypeClient loanTypeClient, CancellationReasonClient cancellationReasonClient, CalendarService calendarService,
                             ServicePointExpirationPeriodService servicePointExpirationPeriodService,
                             HoldingsService holdingsService) {
    super(jdbcTemplate, context, folioSpringLiquibase);

    this.prepareSystemUserService = prepareSystemUserService;
    this.kafkaService = kafkaService;
    this.inventoryClient = inventoryClient;
    this.instanceTypeClient = instanceTypeClient;
    this.locationsClient = locationsClient;
    this.servicePointClient = servicePointClient;
    this.locationUnitClient = locationUnitClient;
    this.loanTypeClient = loanTypeClient;
    this.cancellationReasonClient = cancellationReasonClient;
    this.calendarService = calendarService;
    this.servicePointExpirationPeriodService = servicePointExpirationPeriodService;
    this.holdingsService = holdingsService;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    log.debug("afterTenantUpdate:: parameters tenantAttributes: {}", tenantAttributes);
    prepareSystemUserService.setupSystemUser();
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
    createCalendarIfNotExists();
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
        .holdShelfExpiryPeriod(servicePointExpirationPeriodService.getShelfExpiryPeriod())
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
    holdingsService.fetchDcbHoldingOrCreateIfMissing();
  }

  private void createCancellationReason(){
    try {
      cancellationReasonClient.findCancellationReason(CANCELLATION_REASON_ID);
    } catch (FeignException.NotFound ex) {
      log.debug("createCancellationReason:: creating cancellation reason");
      cancellationReasonClient.createCancellationReason(CancellationReasonClient.CancellationReason.builder()
        .id(CANCELLATION_REASON_ID)
        .description(DCB_CANCELLATION_REASON_NAME)
        .name(DCB_CANCELLATION_REASON_NAME).build());
      log.info("createCancellationReason:: cancellation reason created");
    }
  }

  private void createCalendarIfNotExists() {
    Calendar calendar = calendarService.findCalendarByName(DCB_CALENDAR_NAME);
    if (calendar == null) {
      log.info("createCalendarIfNotExists:: calendar with name {} doesn't exists, so creating new calendar", DCB_CALENDAR_NAME);
      Calendar newCalendar = Calendar.builder()
        .name(DCB_CALENDAR_NAME)
        .startDate(LocalDate.now().toString())
        .endDate(LocalDate.now().plusYears(10).toString())
        .normalHours(List.of(NormalHours.builder()
          .startDay(DayOfWeek.SUNDAY.name())
          .startTime(LocalTime.of(0, 0).toString())
          .endDay(DayOfWeek.SATURDAY.toString())
          .endTime(LocalTime.of(23, 59).toString())
          .build()))
        .assignments(List.of(UUID.fromString(SERVICE_POINT_ID)))
        .exceptions(List.of())
        .build();
      calendarService.createCalendar(newCalendar);
    } else {
      log.info("createCalendarIfNotExists:: calendar with name {} already exists", DCB_CALENDAR_NAME);
    }
  }
}
