package org.folio.dcb.service;

import org.folio.dcb.service.impl.CirculationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;

@ExtendWith(MockitoExtension.class)
public class CirculationServiceTest {

  @InjectMocks
  private CirculationServiceImpl circulationService;

  @Test
  void checkInByBarcodeTest(){
    circulationService.checkInByBarcode(createTransactionEntity());
  }

}
