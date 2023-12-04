package org.folio.dcb.listener;

import org.folio.dcb.controller.BaseIT;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.listener.kafka.CirculationEventListener;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.LendingLibraryServiceImpl;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.getMockDataAsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class CirculationCheckInEventListenerTest extends BaseIT {

  private static final String CHECK_IN_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/check_in.json");

  @MockBean
  private LendingLibraryServiceImpl libraryService;

  @Autowired
  private CirculationEventListener eventListener ;
  @Mock
  private AuthnClient authnClient;
  @MockBean
  private TransactionRepository transactionRepository;

  @Test
  void handleCheckingInWithIncorrectDataTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setRole(LENDER);
    MessageHeaders messageHeaders = getMessageHeaders();
    assertDoesNotThrow(() -> eventListener.handleRequestEvent(null, messageHeaders));
  }

  private MessageHeaders getMessageHeaders() {
    Map<String, Object> header = new HashMap<>();
    header.put(XOkapiHeaders.TENANT, TENANT.getBytes());
    return new MessageHeaders(header);
  }
  @Test
  void handleCheckInEventInPickupFromOpenToAwaitingPickupTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("5b95877d-86c0-4cb7-a0cd-7660b348ae5d");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.OPEN);
    transactionEntity.setRole(PICKUP);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleRequestEvent(CHECK_IN_EVENT_SAMPLE, messageHeaders);
  }
}
