package org.folio.dcb.service.impl;

import static org.folio.dcb.service.ServicePointExpirationPeriodService.getSettingsKey;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbPickup;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.integration.invstorage.ServicePointClient;
import org.folio.dcb.service.CalendarService;
import org.folio.dcb.service.ServicePointExpirationPeriodService;
import org.folio.dcb.service.ServicePointService;
import org.folio.dcb.utils.CqlQuery;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class ServicePointServiceImpl implements ServicePointService {

  public static final String HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT = "Keep_the_current_due_date";

  private final ServicePointClient servicePointClient;
  private final CalendarService calendarService;
  private final ServicePointExpirationPeriodService servicePointExpirationPeriodService;

  @Override
  public ServicePointRequest createServicePointIfNotExists(DcbTransaction dcbTransaction) {
    var dcbPickup = dcbTransaction.getPickup();
    log.debug("createServicePoint:: automate service point creation {} ", dcbPickup);
    var spName = getServicePointName(dcbPickup);
    var query = CqlQuery.exactMatchByName(spName).getQuery();
    var servicePoints = servicePointClient.findByQuery(query).getResult();
    var settingsKey = getSettingsKey(dcbTransaction.getRole().getValue());
    var shelfExpiryPeriod = servicePointExpirationPeriodService.getShelfExpiryPeriod(settingsKey);

    if (servicePoints.isEmpty()) {
      var servicePointId = UUID.randomUUID().toString();
      var servicePointCode = getServicePointCode(dcbPickup);
      log.info("createServicePointIfNotExists:: creating ServicePoint with id {}, name {} and code {}",
        servicePointId, spName, servicePointCode);
      var servicePointRequest = createServicePointRequest(servicePointId, spName, servicePointCode, shelfExpiryPeriod);
      var servicePointResponse = servicePointClient.createServicePoint(servicePointRequest);
      calendarService.addServicePointIdToDefaultCalendar(UUID.fromString(servicePointResponse.getId()));
      return servicePointResponse;
    } else {
      log.info("createServicePointIfNotExists:: servicePoint Exists with name {}, hence reusing it", spName);
      var servicePointId = UUID.fromString(servicePoints.getFirst().getId());
      calendarService.associateServicePointIdWithDefaultCalendarIfAbsent(servicePointId);
      var servicePointRequest = servicePoints.getFirst();
      servicePointRequest.setHoldShelfExpiryPeriod(shelfExpiryPeriod);
      servicePointClient.updateServicePointById(servicePointRequest.getId(), servicePointRequest);
      return servicePointRequest;
    }
  }

  private ServicePointRequest createServicePointRequest(String id, String name, String code,
    HoldShelfExpiryPeriod shelfExpiryPeriod) {
    return ServicePointRequest.builder()
      .id(id)
      .name(name)
      .code(code)
      .discoveryDisplayName(name)
      .pickupLocation(true)
      .holdShelfExpiryPeriod(shelfExpiryPeriod)
      .holdShelfClosedLibraryDateManagement(HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT)
      .build();
  }

  private static String getServicePointName(DcbPickup dcbPickup) {
    return String.format("DCB_%s_%s", dcbPickup.getLibraryCode(), dcbPickup.getServicePointName());
  }

  private static String getServicePointCode(DcbPickup dcbPickup) {
    return String.format("DCB_%s_%s", dcbPickup.getLibraryCode(), dcbPickup.getServicePointName())
      .replaceAll("\\s+", "")
      .toUpperCase();
  }
}
