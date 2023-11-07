package org.folio.dcb.service;

import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.CirculationServiceImpl;
import org.folio.dcb.service.impl.PickupLibraryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;

import static org.mockito.Mockito.times;
@ExtendWith(MockitoExtension.class)
class PickupLibraryServiceTest {

  @InjectMocks
  private PickupLibraryServiceImpl pickupLibraryService;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private CirculationServiceImpl circulationService;
  @Test
  void updateTransactionTestFromCreatedToOpen() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(PICKUP);

    pickupLibraryService.updateTransactionStatus(transactionEntity, TransactionStatus.builder().status(TransactionStatus.StatusEnum.OPEN).build());
    Mockito.verify(transactionRepository, times(1)).save(transactionEntity);
  }

}
