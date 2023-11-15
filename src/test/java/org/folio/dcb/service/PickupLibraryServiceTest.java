package org.folio.dcb.service;

import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.service.impl.PickupLibraryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.PICKUP_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.createDcbItem;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createDefaultDcbPatron;
import static org.folio.dcb.utils.EntityUtils.createUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickupLibraryServiceTest {

  @InjectMocks
  private PickupLibraryServiceImpl pickupLibraryService;
  @Mock
  private UserService userService;
  @Mock
  private RequestService requestService;
  @Mock
  private CirculationItemService circulationItemService;
  @Mock
  private CirculationService circulationService;

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
