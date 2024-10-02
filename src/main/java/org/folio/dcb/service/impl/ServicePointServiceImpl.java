package org.folio.dcb.service.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.domain.dto.DcbPickup;
import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.service.CalendarService;
import org.folio.dcb.service.ServicePointService;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class ServicePointServiceImpl implements ServicePointService {

  private final InventoryServicePointClient servicePointClient;
  private final CalendarService calendarService;
  public static final String HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT = "Keep_the_current_due_date";

  @Override
  public ServicePointRequest createServicePointIfNotExists(DcbPickup pickupServicePoint) {
    log.debug("createServicePoint:: automate service point creation {} ", pickupServicePoint);
    String servicePointName = getServicePointName(pickupServicePoint.getLibraryCode(),
      pickupServicePoint.getServicePointName());
    var servicePointRequestList = servicePointClient
      .getServicePointByName(servicePointName).getResult();
    if (servicePointRequestList.isEmpty()) {
      String servicePointId = UUID.randomUUID().toString();
      String servicePointCode = getServicePointCode(pickupServicePoint.getLibraryCode(),
        pickupServicePoint.getServicePointName());
      log.info("createServicePointIfNotExists:: creating ServicePoint with id {}, name {} and code {}",
        servicePointId, servicePointName, servicePointCode);
      var servicePointRequest = createServicePointRequest(servicePointId,
        servicePointName, servicePointCode);
      ServicePointRequest servicePointResponse = servicePointClient.createServicePoint(servicePointRequest);
      calendarService.addServicePointIdToDefaultCalendar(UUID.fromString(servicePointResponse.getId()));
      return servicePointResponse;
    } else {
      log.info("createServicePointIfNotExists:: servicePoint Exists with name {}, hence reusing it", servicePointName);
      calendarService.associateServicePointIdWithDefaultCalendarIfAbsent(UUID.fromString(servicePointRequestList.get(0).getId()));
      return servicePointRequestList.get(0);
    }
  }

  private ServicePointRequest createServicePointRequest(String id, String name, String code){
    return ServicePointRequest.builder()
      .id(id)
      .name(name)
      .code(code)
      .discoveryDisplayName(name)
      .pickupLocation(true)
      .holdShelfExpiryPeriod(HoldShelfExpiryPeriod.builder().duration(3).intervalId(HoldShelfExpiryPeriod.IntervalIdEnum.DAYS).build())
      .holdShelfClosedLibraryDateManagement(HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT)
      .build();
  }

  private String getServicePointName(String libraryCode, String servicePointName){
    return String.format("DCB_%s_%s", libraryCode, servicePointName);
  }

  private String getServicePointCode(String libraryCode, String servicePointName){
    return String.format("DCB_%s_%s", libraryCode, servicePointName).replaceAll("\\s+","").toUpperCase();
  }
}
