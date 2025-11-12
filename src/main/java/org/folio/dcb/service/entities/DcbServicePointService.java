package org.folio.dcb.service.entities;

import static org.folio.dcb.service.impl.ServicePointServiceImpl.HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT;
import static org.folio.dcb.utils.CqlQuery.exactMatchByName;
import static org.folio.dcb.utils.DCBConstants.CODE;
import static org.folio.dcb.utils.DCBConstants.DEFAULT_PERIOD;
import static org.folio.dcb.utils.DCBConstants.NAME;
import static org.folio.dcb.utils.DCBConstants.SERVICE_POINT_ID;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.service.ServicePointExpirationPeriodService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DcbServicePointService implements DcbEntityService<ServicePointRequest> {

  private final InventoryServicePointClient servicePointClient;
  private final ServicePointExpirationPeriodService servicePointExpirationPeriodService;

  @Override
  public Optional<ServicePointRequest> findDcbEntity() {
    var servicePointsByQuery = servicePointClient.findByQuery(exactMatchByName(NAME));
    return findFirstValue(servicePointsByQuery);
  }

  @Override
  public ServicePointRequest createDcbEntity() {
    log.debug("createDcbEntity:: Creating a new DCB Service Point");
    var shelfExpiryPeriod = servicePointExpirationPeriodService.getShelfExpiryPeriod();
    var dcbServicePoint = getDcbServicePoint(shelfExpiryPeriod);
    var createdServicePoint = servicePointClient.createServicePoint(dcbServicePoint);
    log.info("createDcbEntity:: DCB Service Point created");
    return createdServicePoint;
  }

  @Override
  public ServicePointRequest getDefaultValue() {
    return getDcbServicePoint(DEFAULT_PERIOD);
  }

  private ServicePointRequest getDcbServicePoint(HoldShelfExpiryPeriod expiryPeriod) {
    return ServicePointRequest.builder()
      .id(SERVICE_POINT_ID)
      .name(NAME)
      .code(CODE)
      .discoveryDisplayName(NAME)
      .pickupLocation(true)
      .holdShelfExpiryPeriod(expiryPeriod)
      .holdShelfClosedLibraryDateManagement(HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT)
      .build();
  }
}
