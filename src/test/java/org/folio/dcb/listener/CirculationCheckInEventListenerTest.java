package org.folio.dcb.listener;

import org.folio.dcb.controller.BaseIT;
import org.folio.dcb.service.LibraryService;
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

import static org.folio.dcb.utils.EntityUtils.getMockDataAsString;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;

@SpringBootTest
class CirculationCheckInEventListenerTest extends BaseIT {

  private static final String CHECK_IN_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/check_in.json");

  @MockBean
  private LibraryService libraryService;
  @Autowired
  private CirculationCheckInEventListener eventListener ;
  @Mock
  private AuthnClient authnClient;

  @Test
  void shouldExists() {
    MessageHeaders messageHeaders = getMessageHeaders();
    eventListener.handleCheckingIn(CHECK_IN_EVENT_SAMPLE, messageHeaders);
    Mockito.verify(libraryService, times(1)).updateTransactionStatus(anyString());
  }

  private MessageHeaders getMessageHeaders() {
    Map<String, Object> header = new HashMap<>();
    header.put(XOkapiHeaders.TENANT, TENANT.getBytes());
    return new MessageHeaders(header);
  }
}
