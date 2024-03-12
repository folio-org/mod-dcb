package org.folio.dcb.service;

import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.folio.dcb.service.impl.PickupLibraryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.PICKUP_SERVICE_POINT_ID;
import static org.folio.dcb.utils.EntityUtils.createCirculationItem;
import static org.folio.dcb.utils.EntityUtils.createCirculationRequest;
import static org.folio.dcb.utils.EntityUtils.createDcbItem;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createDefaultDcbPatron;
import static org.folio.dcb.utils.EntityUtils.createUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
  @Mock
  private BaseLibraryService baseLibraryService;

  @Test
  void createTransactionTest() {
    var item = createDcbItem();
    var patron = createDefaultDcbPatron();
    var user = createUser();
    var circulationItem = createCirculationItem();
    circulationItem.setId(item.getId());

    when(userService.fetchOrCreateUser(any()))
      .thenReturn(user);
    when(requestService.createHoldItemRequest(any(), any(), anyString())).thenReturn(createCirculationRequest());
    when(circulationItemService.checkIfItemExistsAndCreate(any(), any())).thenReturn(circulationItem);
    doNothing().when(baseLibraryService).saveDcbTransaction(any(), any(), any());

    var response = pickupLibraryService.createCirculation(DCB_TRANSACTION_ID, createDcbTransactionByRole(PICKUP));
    verify(userService).fetchOrCreateUser(patron);
    verify(circulationItemService).checkIfItemExistsAndCreate(item, PICKUP_SERVICE_POINT_ID);
    verify(requestService).createHoldItemRequest(user, item, PICKUP_SERVICE_POINT_ID);

    assertEquals(TransactionStatusResponse.StatusEnum.CREATED, response.getStatus());
    assertEquals(item, response.getItem());
    assertEquals(patron, response.getPatron());
  }

}
