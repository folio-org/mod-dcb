package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.domain.dto.DcbPickup;
import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.service.ServicePointService;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class ServicePointServiceImpl implements ServicePointService {

  private final InventoryServicePointClient servicePointClient;

  @Override
  public ServicePointRequest createServicePoint(DcbPickup pickupServicePoint) {
    log.debug("createServicePoint:: automate service point creation {} ", pickupServicePoint);

    ServicePointRequest servicePointRequest = createServicePointRequest(
      pickupServicePoint.getServicePointId(),
      getServicePointName(pickupServicePoint.getLibraryName(), pickupServicePoint.getServicePointName()),
      getServicePointCode(pickupServicePoint.getLibraryName(), pickupServicePoint.getServicePointName()),
      getServicePointName(pickupServicePoint.getLibraryName(), pickupServicePoint.getServicePointName())
    );

     try{
       return servicePointClient.createServicePoint(servicePointRequest);
     } catch (ResourceAlreadyExistException e){
       log.debug("Service point already exists");
       return servicePointRequest;
     }
  }

  private ServicePointRequest createServicePointRequest(String id, String name, String code, String discoveryDisplayName){
    return ServicePointRequest.builder()
      .id(id)
      .name(name)
      .code(code)
      .discoveryDisplayName(discoveryDisplayName)
      .pickupLocation(true)
      .holdShelfExpiryPeriod(HoldShelfExpiryPeriod.builder().duration(3).intervalId(HoldShelfExpiryPeriod.IntervalIdEnum.DAYS).build())
      .holdShelfClosedLibraryDateManagement(HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT)
      .build();
  }

  private String getServicePointName(String libraryName, String servicePointName){
    return String.format("DCB_%s_%s", libraryName, servicePointName);
  }

  private String getServicePointCode(String libraryName, String servicePointName){
    return String.format("DCB_%s_%s", libraryName, servicePointName).replaceAll("\\s+","").toUpperCase();
  }
}
