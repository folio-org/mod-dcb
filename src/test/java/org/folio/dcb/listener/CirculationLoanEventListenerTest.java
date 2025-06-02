package org.folio.dcb.listener;

import org.folio.dcb.controller.BaseIT;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.listener.kafka.CirculationEventListener;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.folio.dcb.service.impl.BorrowingPickupLibraryServiceImpl;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.integration.XOkapiHeaders;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.getMockDataAsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class CirculationLoanEventListenerTest extends BaseIT {

  private static final String CHECK_OUT_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/check_out.json");
  private static final String LOAN_EVENT_WITH_OUT_IS_DCB = getMockDataAsString("mockdata/kafka/loan_sample_without_isdcb.json");
  private static final String CHECK_OUT_EVENT_SAMPLE_FOR_DCB = getMockDataAsString("mockdata/kafka/check_out_dcb.json");
  private static final String CHECK_IN_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/loan_check_in.json");
  private static final String CHECK_IN_DCB_FALSE_LOAN_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/loan_check_in_dcb_false.json");
  private static final String NO_ITEM_ID_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/loan_check_in_no_item_id.json");
  private static final String CHECK_IN_UNDEFINED_SAMPLE = getMockDataAsString("mockdata/kafka/loan_undefined.json");

  @Captor
  private ArgumentCaptor<TransactionEntity> transactionEntityArgumentCaptor;
  @Captor
  private ArgumentCaptor<TransactionStatus.StatusEnum> statusEnumArgumentCaptor;

  @Mock
  private BorrowingPickupLibraryServiceImpl libraryService;
  @Autowired
  private CirculationEventListener eventListener ;
  @Mock
  private AuthnClient authnClient;
  @MockitoBean
  private TransactionRepository transactionRepository;

  @MockitoSpyBean
  private BaseLibraryService baseLibraryService;

  @Test
  void handleCheckingOutForNonDcbTest() {
    MessageHeaders messageHeaders = getMessageHeaders();
    eventListener.handleLoanEvent(CHECK_OUT_EVENT_SAMPLE, messageHeaders);
    verify(transactionRepository, times(0)).save(any());
  }

  @Test
  void handleLoanEventWhenIsDcbFieldIsNotPresent() {
    MessageHeaders messageHeaders = getMessageHeaders();
    eventListener.handleLoanEvent(LOAN_EVENT_WITH_OUT_IS_DCB, messageHeaders);
    verify(transactionRepository, times(0)).save(any());
  }

  @Test
  void handleCheckingOutForDcbTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setRole(BORROWING_PICKUP);
    transactionEntity.setStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    transactionEntity.setItemId("8db107f5-12aa-479f-9c07-39e7c9cf2e4d");
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleLoanEvent(CHECK_OUT_EVENT_SAMPLE_FOR_DCB, messageHeaders);
    verify(transactionRepository).save(any());
  }


  @Test
  void handleCheckInEventInBorrowingFromOpenToAwaitingPickup_1() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("8db107f5-12aa-479f-9c07-39e7c9cf2e4d");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.AWAITING_PICKUP);
    transactionEntity.setRole(BORROWING_PICKUP);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleLoanEvent(CHECK_OUT_EVENT_SAMPLE, messageHeaders);
    verify(transactionRepository, times(0)).save(any());
  }

  @Test
  void handleCheckInEventInPickupFromItemCheckedOutToCheckedIn() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("8db107f5-12aa-479f-9c07-39e7c9cf2e4d");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    transactionEntity.setRole(PICKUP);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleLoanEvent(CHECK_OUT_EVENT_SAMPLE_FOR_DCB, messageHeaders);
    verify(transactionRepository).save(any());
  }

  @Test
  void handleEmptyEventTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("8db107f5-12aa-479f-9c07-39e7c9cf2e4d");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    transactionEntity.setRole(PICKUP);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleLoanEvent(NO_ITEM_ID_EVENT_SAMPLE, messageHeaders);
    verify(transactionRepository, times(0)).save(any());
  }

  @Test
  void handleCheckInEventInBorrowingPickupFromItemCheckedOutToCheckedIn() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("8db107f5-12aa-479f-9c07-39e7c9cf2e4d");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    transactionEntity.setRole(BORROWING_PICKUP);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleLoanEvent(CHECK_OUT_EVENT_SAMPLE_FOR_DCB, messageHeaders);
    verify(transactionRepository).save(any());
  }

  @Test
  void handleCheckInEventInLenderFromItemCheckedInToClosedIn() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("8db107f5-12aa-479f-9c07-39e7c9cf2e4d");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    transactionEntity.setRole(LENDER);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleLoanEvent(CHECK_IN_EVENT_SAMPLE, messageHeaders);
    verify(transactionRepository).save(any());
  }

  @Test
  void handleUndefinedEvent() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("8db107f5-12aa-479f-9c07-39e7c9cf2e4d");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    transactionEntity.setRole(LENDER);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleLoanEvent(CHECK_IN_UNDEFINED_SAMPLE, messageHeaders);
    verify(transactionRepository, times(0)).save(any());
  }

  @Test
  void handleCheckingInWithIncorrectDataTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setRole(BORROWING_PICKUP);
    MessageHeaders messageHeaders = getMessageHeaders();
    assertDoesNotThrow(() -> eventListener.handleLoanEvent(null, messageHeaders));
  }

  @Test
  void handleCheckOutEventInBorrowingPickupSelfBorrowingTrueCheckout_positive() {
    var transactionEntity = getBorrowingPickupWithSelfBorrowingTransactionEntity(Boolean.TRUE);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findSingleTransactionsByItemIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleLoanEvent(CHECK_OUT_EVENT_SAMPLE, messageHeaders);
    verify(transactionRepository).save(any());

    verify(baseLibraryService).updateTransactionEntity(transactionEntityArgumentCaptor.capture(),
            statusEnumArgumentCaptor.capture());
    TransactionEntity transactionEntityArgumentCaptorValue = transactionEntityArgumentCaptor.getValue();
    assertEquals(BORROWING_PICKUP, transactionEntityArgumentCaptorValue.getRole());
    assertEquals(Boolean.TRUE, transactionEntityArgumentCaptorValue.getSelfBorrowing());
    assertEquals(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT, statusEnumArgumentCaptor.getValue());
  }

  @Test
  void handleCheckOutEventInBorrowingPickupSelfBorrowingFalseCheckout_negative() {
    var transactionEntity = getBorrowingPickupWithSelfBorrowingTransactionEntity(Boolean.FALSE);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findSingleTransactionsByItemIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleLoanEvent(CHECK_OUT_EVENT_SAMPLE, messageHeaders);
    verify(transactionRepository, never()).save(any());
  }

  @Test
  void handleCheckOutEventInBorrowingPickupSelfBorrowingTrueCheckin_positive() {
    var transactionEntity = getBorrowingPickupWithSelfBorrowingTransactionEntity(Boolean.TRUE);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findSingleTransactionsByItemIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleLoanEvent(CHECK_IN_DCB_FALSE_LOAN_EVENT_SAMPLE, messageHeaders);
    verify(transactionRepository).save(any());

    verify(baseLibraryService).updateTransactionEntity(transactionEntityArgumentCaptor.capture(),
            statusEnumArgumentCaptor.capture());
    TransactionEntity transactionEntityArgumentCaptorValue = transactionEntityArgumentCaptor.getValue();
    assertEquals(BORROWING_PICKUP, transactionEntityArgumentCaptorValue.getRole());
    assertEquals(Boolean.TRUE, transactionEntityArgumentCaptorValue.getSelfBorrowing());
    assertEquals(TransactionStatus.StatusEnum.CLOSED, statusEnumArgumentCaptor.getValue());
  }

  @Test
  void handleCheckOutEventInBorrowingPickupSelfBorrowingTrueCheckin_negative() {
    var transactionEntity = getBorrowingPickupWithSelfBorrowingTransactionEntity(Boolean.FALSE);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findSingleTransactionsByItemIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleLoanEvent(CHECK_IN_DCB_FALSE_LOAN_EVENT_SAMPLE, messageHeaders);
    verify(transactionRepository, never()).save(any());
  }

  private static @NotNull TransactionEntity getBorrowingPickupWithSelfBorrowingTransactionEntity(Boolean selfBorrowing) {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(BORROWING_PICKUP);
    transactionEntity.setSelfBorrowing(selfBorrowing);
    return transactionEntity;
  }

  private MessageHeaders getMessageHeaders() {
    Map<String, Object> header = new HashMap<>();
    header.put(XOkapiHeaders.TENANT, TENANT.getBytes());
    return new MessageHeaders(header);
  }
}
