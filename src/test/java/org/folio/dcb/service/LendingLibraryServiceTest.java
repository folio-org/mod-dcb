package org.folio.dcb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;
import static org.folio.dcb.utils.EntityUtils.CIRCULATION_REQUEST_ID;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.createCirculationRequest;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createServicePointRequest;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.createTransactionStatus;
import static org.folio.dcb.utils.EntityUtils.createUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatus.StatusEnum;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.folio.dcb.service.impl.CirculationServiceImpl;
import org.folio.dcb.service.impl.LendingLibraryServiceImpl;
import org.folio.dcb.service.impl.RequestServiceImpl;
import org.folio.dcb.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LendingLibraryServiceTest {

  @InjectMocks private LendingLibraryServiceImpl lendingLibraryService;
  @Mock private TransactionRepository transactionRepository;
  @Mock private UserServiceImpl userService;
  @Mock private RequestServiceImpl requestService;
  @Mock private CirculationServiceImpl circulationService;
  @Mock private BaseLibraryService baseLibraryService;
  @Mock private ServicePointService servicePointService;
  @Mock private ItemService itemService;

  @Test
  void createTransactionTest() {
    var dcbTransaction = createDcbTransactionByRole(LENDER);
    assertThat(dcbTransaction.getPatron()).isNotNull();
    assertThat(dcbTransaction.getItem()).isNotNull();
    assertThat(dcbTransaction.getPickup()).isNotNull();

    var user = createUser();
    var servicePoint = createServicePointRequest().id(UUID.randomUUID().toString());

    when(userService.fetchOrCreateUser(any())).thenReturn(user);
    when(servicePointService.createServicePointIfNotExists(dcbTransaction)).thenReturn(servicePoint);
    when(requestService.createRequestBasedOnItemStatus(any(), any(), any())).thenReturn(createCirculationRequest());
    doNothing().when(baseLibraryService).saveDcbTransaction(any(), any(), any());

    var response = lendingLibraryService.createCirculation(DCB_TRANSACTION_ID, dcbTransaction);

    assertThat(response.getStatus()).isEqualTo(TransactionStatusResponse.StatusEnum.CREATED);
    verify(userService).fetchOrCreateUser(dcbTransaction.getPatron());
    verify(requestService).createRequestBasedOnItemStatus(
      user, dcbTransaction.getItem(), dcbTransaction.getPickup().getServicePointId());
    verify(baseLibraryService).saveDcbTransaction(DCB_TRANSACTION_ID, dcbTransaction, CIRCULATION_REQUEST_ID);
    assertThat(dcbTransaction.getPickup().getServicePointId()).isEqualTo(servicePoint.getId());
  }

  @Test
  void transactionStatusFromOpenToAwaitingTest() {
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.OPEN);
    doNothing().when(circulationService).checkInByBarcode(dcbTransaction);
    var awaitingPickupStatus = TransactionStatus.builder().status(StatusEnum.AWAITING_PICKUP).build();

    lendingLibraryService.updateTransactionStatus(dcbTransaction, awaitingPickupStatus);

    assertThat(dcbTransaction.getStatus()).isEqualTo(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    verify(circulationService).checkInByBarcode(dcbTransaction);
  }

  @Test
  void transactionStatusFromAwaitingToCheckoutTest() {
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    doNothing().when(circulationService).checkOutByBarcode(dcbTransaction);
    var checkedOutStatus = TransactionStatus.builder().status(StatusEnum.ITEM_CHECKED_OUT).build();

    lendingLibraryService.updateTransactionStatus(dcbTransaction, checkedOutStatus);

    verify(circulationService).checkOutByBarcode(dcbTransaction);
    assertThat(dcbTransaction.getStatus()).isEqualTo(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
  }

  @Test
  void transactionStatusFromCheckoutToCheckInTest() {
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    var checkedInStatus = TransactionStatus.builder().status(StatusEnum.ITEM_CHECKED_IN).build();
    lendingLibraryService.updateTransactionStatus(dcbTransaction, checkedInStatus);
    assertThat(dcbTransaction.getStatus()).isEqualTo(TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
  }

  @Test
  void transactionStatusFromCreatedToCancelledTest() {
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(TransactionStatus.StatusEnum.CREATED);
    var cancelledStatus = TransactionStatus.builder().status(StatusEnum.CANCELLED).build();
    lendingLibraryService.updateTransactionStatus(dcbTransaction, cancelledStatus);
    verify(baseLibraryService).cancelTransactionRequest(dcbTransaction);
  }

  @Test
  void updateTransactionWithWrongStatusTest() {
    var transactionEntity = createTransactionEntity();
    var transactionStatus = createTransactionStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    assertThatThrownBy(() -> lendingLibraryService.updateTransactionStatus(transactionEntity, transactionStatus))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testUpdateTransactionStatusFromCreatedToOpenShouldUpdateEntity() {
    // TestMate-1bbc6de8d11f860bac864b25fad8e5ad
    var dcbTransaction = createTransactionEntity();
    dcbTransaction.setStatus(CREATED);
    var transactionStatus = createTransactionStatus(OPEN);

    lendingLibraryService.updateTransactionStatus(dcbTransaction, transactionStatus);

    assertThat(dcbTransaction.getStatus()).isEqualTo(OPEN);
    verify(transactionRepository).save(dcbTransaction);
  }
}
