package org.folio.dcb.listener;

import org.folio.dcb.controller.BaseIT;
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

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.getMockDataAsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
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
    eventListener.handleCheckingIn(CHECK_IN_EVENT_SAMPLE, messageHeaders);
    Mockito.verify(libraryService, times(1)).updateStatusByTransactionEntity(any());
  }

  @Test
  void handleCheckingInWithIncorrectDataTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setRole(LENDER);
    MessageHeaders messageHeaders = getMessageHeaders();
    assertDoesNotThrow(() -> eventListener.handleCheckingIn(null, messageHeaders));
  }

  private MessageHeaders getMessageHeaders() {
    Map<String, Object> header = new HashMap<>();
    header.put(XOkapiHeaders.TENANT, TENANT.getBytes());
    return new MessageHeaders(header);
  }
}
