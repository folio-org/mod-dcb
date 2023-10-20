package org.folio.dcb.listener;

import org.folio.dcb.controller.BaseIT;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.LendingLibraryServiceImpl;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.getMockDataAsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@SpringBootTest
class CirculationCheckInEventListenerTest extends BaseIT {

  private static final String CHECK_IN_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/check_in.json");

  @MockBean
  private LendingLibraryServiceImpl libraryService;

  @Autowired
  private CirculationCheckInEventListener eventListener ;
  @Mock
  private AuthnClient authnClient;
  @MockBean
  private TransactionRepository transactionRepository;

  @Test
  void handleCheckingInTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setRole(LENDER);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByItemId(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleCheckInEvent(CHECK_IN_EVENT_SAMPLE, messageHeaders);
    Mockito.verify(libraryService, times(1)).updateStatusByTransactionEntity(any());
  }

  @Test
  void handleCheckInEventInBorrowingFromOpenToAwaitingPickup_1() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("5b95877d-86c0-4cb7-a0cd-7660b348ae5b");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.OPEN);
    transactionEntity.setRole(BORROWER);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByItemId(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleCheckInEvent(CHECK_IN_EVENT_SAMPLE, messageHeaders);
    Mockito.verify(transactionRepository).save(any());
  }

  @Test
  void handleCheckInEventInBorrowingFromOpenToAwaitingPickup_2() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("5b95877d-86c0-4cb7-a0cd-7660b348ae5c");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.OPEN);
    transactionEntity.setRole(BORROWER);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByItemId(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleCheckInEvent(CHECK_IN_EVENT_SAMPLE, messageHeaders);
    Mockito.verify(transactionRepository, never()).save(any());
  }

  @Test
  void handleCheckingInWithIncorrectDataTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setRole(LENDER);
    MessageHeaders messageHeaders = getMessageHeaders();
    assertDoesNotThrow(() -> eventListener.handleCheckInEvent(null, messageHeaders));
  }

  private MessageHeaders getMessageHeaders() {
    Map<String, Object> header = new HashMap<>();
    header.put(XOkapiHeaders.TENANT, TENANT.getBytes());
    return new MessageHeaders(header);
  }
}
