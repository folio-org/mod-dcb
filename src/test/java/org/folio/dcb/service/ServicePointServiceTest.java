package org.folio.dcb.service;

import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.service.impl.ServicePointServiceImpl;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.folio.dcb.utils.EntityUtils.createDcbPickup;
import static org.folio.dcb.utils.EntityUtils.createServicePointRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicePointServiceTest {

  @InjectMocks
  private ServicePointServiceImpl servicePointService;

  @Mock
  private InventoryServicePointClient inventoryServicePointClient;

  @Test
  void createServicePointIfNotExistsTest(){
    when(inventoryServicePointClient.getServicePointByName(any()))
      .thenReturn(ResultList.of(0, List.of()));
    when(inventoryServicePointClient.createServicePoint(any()))
      .thenReturn(createServicePointRequest());
    servicePointService.createServicePointIfNotExists(createDcbPickup());
    verify(inventoryServicePointClient).createServicePoint(any());
    verify(inventoryServicePointClient).getServicePointByName(any());
  }

  @Test
  void createServicePointIfExistsTest(){
    var servicePointRequest = createServicePointRequest();
    var servicePointId = UUID.randomUUID().toString();
    servicePointRequest.setId(servicePointId);
    when(inventoryServicePointClient.getServicePointByName(any()))
      .thenReturn(ResultList.of(0, List.of(servicePointRequest)));
    var response = servicePointService.createServicePointIfNotExists(createDcbPickup());
    assertEquals(servicePointId, response.getId());
    verify(inventoryServicePointClient, never()).createServicePoint(any());
    verify(inventoryServicePointClient).getServicePointByName(any());
  }

}
