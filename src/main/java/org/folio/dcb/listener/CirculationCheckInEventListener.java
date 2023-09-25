package org.folio.dcb.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.service.LibraryService;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import static org.folio.dcb.utils.TransactionHelper.getHeaderValue;

@Log4j2
@Component
@RequiredArgsConstructor
public class CirculationCheckInEventListener {
  public final LibraryService libraryService;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;
  public static final String CHECK_IN_LISTENER_ID = "check-in-listener-id";

  @KafkaListener(
    id = CHECK_IN_LISTENER_ID,
    topicPattern = "#{folioKafkaProperties.listener['check-in'].topicPattern}",
    concurrency = "#{folioKafkaProperties.listener['check-in'].concurrency}",
    containerFactory = "kafkaListenerContainerFactory")
  public void handleCheckingIn(String data, MessageHeaders messageHeaders) {
    String requestedTenantId = getHeaderValue(messageHeaders, XOkapiHeaders.TENANT, null).get(0);
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(requestedTenantId, () -> libraryService.updateTransactionStatus(data));
  }
}
