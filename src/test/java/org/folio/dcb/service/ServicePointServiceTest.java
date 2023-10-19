package org.folio.dcb.service;

import feign.FeignException;
import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.service.impl.ServicePointServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.utils.EntityUtils.createDcbPickup;
import static org.folio.dcb.utils.EntityUtils.createServicePointRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicePointServiceTest {

  @InjectMocks
  private ServicePointServiceImpl servicePointService;

  @Mock
  private InventoryServicePointClient inventoryServicePointClient;

  @Test
  void createServicePointTest(){
    when(inventoryServicePointClient.createServicePoint(any())).thenReturn(createServicePointRequest());
    assertEquals(servicePointService.createServicePoint(createDcbPickup()), createServicePointRequest());
  }

  @Test
  void returnServicePointRequestWhenServicePointExist(){
    String url = "http://service-points";
    when(inventoryServicePointClient.createServicePoint(any())).thenThrow(new FeignException.UnprocessableEntity("Service Point Exists", generateRequest(url), null, null));
    assertEquals(servicePointService.createServicePoint(createDcbPickup()), createServicePointRequest());
  }

  feign.Response generateResponse(String url,String message, int status){
    return feign.Response.builder()
      .status(status)
      .request(feign.Request.create(feign.Request.HttpMethod.GET, url, new java.util.HashMap<>(), null, new feign.RequestTemplate()))
      .body(message, java.nio.charset.StandardCharsets.UTF_8)
      .build();
  }

  feign.Request generateRequest(String url){
    return feign.Request.create(feign.Request.HttpMethod.GET, url, new java.util.HashMap<>(), null, new feign.RequestTemplate());
  }


}
