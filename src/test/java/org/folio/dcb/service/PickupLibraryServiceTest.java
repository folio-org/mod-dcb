package org.folio.dcb.service;

import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.PickupLibraryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.PICKUP_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.createDcbItem;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createDefaultDcbPatron;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.createTransactionStatus;
import static org.folio.dcb.utils.EntityUtils.createUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickupLibraryServiceTest {

  @InjectMocks
  private PickupLibraryServiceImpl pickupLibraryService;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private UserService userService;
  @Mock
  private RequestService requestService;
  @Mock
  private CirculationItemService circulationItemService;
  @Mock
  private CirculationService circulationService;

  @Test
  void updateTransactionTestFromCreatedToOpen() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(PICKUP);

    pickupLibraryService.updateTransactionStatus(transactionEntity, TransactionStatus.builder().status(TransactionStatus.StatusEnum.OPEN).build());
    Mockito.verify(transactionRepository, times(1)).save(transactionEntity);
  }

  @Test
  void updateTransactionWithWrongStatusTest() {
    TransactionEntity transactionEntity = createTransactionEntity();
    TransactionStatus transactionStatus = createTransactionStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    assertThrows(IllegalArgumentException.class, () -> pickupLibraryService.updateTransactionStatus(transactionEntity, transactionStatus));
  }

  @Test
  void createTransactionTest() {
    var item = createDcbItem();
    var patron = createDefaultDcbPatron();
    var user = createUser();

    when(userService.fetchOrCreateUser(any()))
      .thenReturn(user);
    doNothing().when(requestService).createHoldItemRequest(any(), any(), any());
    doNothing().when(circulationItemService).checkIfItemExistsAndCreate(any(), any());

    var response = pickupLibraryService.createCirculation(DCB_TRANSACTION_ID, createDcbTransactionByRole(PICKUP), PICKUP_SERVICE_POINT_ID);
    verify(userService).fetchOrCreateUser(patron);
    verify(circulationItemService).checkIfItemExistsAndCreate(item, PICKUP_SERVICE_POINT_ID);
    verify(requestService).createHoldItemRequest(user, item, PICKUP_SERVICE_POINT_ID);

    assertEquals(TransactionStatusResponse.StatusEnum.CREATED, response.getStatus());
    assertEquals(item, response.getItem());
    assertEquals(patron, response.getPatron());
  }

}
