package org.folio.dcb.service;

import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.service.impl.CirculationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CirculationServiceTest {

  @InjectMocks
  private CirculationServiceImpl circulationService;

  @Mock
  private CirculationClient circulationClient;

  @Test
  void checkInByBarcodeTest(){
    circulationService.checkInByBarcode(createTransactionEntity());
    verify(circulationClient).checkInByBarcode(any());
  }

  @Test
  void checkInByBarcodeWithServicePointTest(){
    circulationService.checkInByBarcode(createTransactionEntity(), String.valueOf(UUID.randomUUID()));
    verify(circulationClient).checkInByBarcode(any());
  }

}
