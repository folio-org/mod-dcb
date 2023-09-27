package org.folio.dcb.service;

import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.service.impl.CirculationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;

@ExtendWith(MockitoExtension.class)
class CirculationServiceTest {

  @InjectMocks
  private CirculationServiceImpl circulationService;

  @Mock
  private CirculationClient circulationClient;

  @Test
  void checkInByBarcodeTest(){
    circulationService.checkInByBarcode(createTransactionEntity());
  }

}
