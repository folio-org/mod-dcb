package org.folio.dcb.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.domain.dto.DcbPickup;
import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.service.ServicePointService;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class ServicePointServiceImpl implements ServicePointService {

  private final InventoryServicePointClient servicePointClient;
  public static final String HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT = "Keep_the_current_due_date";

  @Override
  public ServicePointRequest createServicePoint(DcbPickup pickupServicePoint) {
    log.debug("createServicePoint:: automate service point creation {} ", pickupServicePoint);

    ServicePointRequest servicePointRequest = createServicePointRequest(
      pickupServicePoint.getServicePointId(),
      getServicePointName(pickupServicePoint.getLibraryCode(), pickupServicePoint.getServicePointName()),
      getServicePointCode(pickupServicePoint.getLibraryCode(), pickupServicePoint.getServicePointName())
    );

     try{
       return servicePointClient.createServicePoint(servicePointRequest);
     } catch (FeignException.UnprocessableEntity e){
       if(e.getMessage().contains("Service Point Exists")){
         log.warn("Service point already exists");
         return servicePointRequest;
       } else{
         throw new IllegalArgumentException(e);
       }
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
