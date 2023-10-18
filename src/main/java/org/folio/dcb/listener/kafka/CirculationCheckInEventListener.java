package org.folio.dcb.listener.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.LibraryService;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static org.folio.dcb.utils.TransactionHelper.getHeaderValue;
import static org.folio.dcb.utils.TransactionHelper.parseCheckInEvent;

@Log4j2
@Component
@RequiredArgsConstructor
public class CirculationCheckInEventListener {
  public static final String CHECK_IN_LISTENER_ID = "check-in-listener-id";
  @Qualifier("lendingLibraryService")
  private final LibraryService lendingLibraryService;
  private final TransactionRepository transactionRepository;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;

  @KafkaListener(
    id = CHECK_IN_LISTENER_ID,
    topicPattern = "#{folioKafkaProperties.listener['check-in'].topicPattern}",
    concurrency = "#{folioKafkaProperties.listener['check-in'].concurrency}")
  public void handleCheckInEvent(String data, MessageHeaders messageHeaders) {
    String tenantId = getHeaderValue(messageHeaders, XOkapiHeaders.TENANT, null).get(0);
    var checkInItemId = parseCheckInEvent(data);
    if (Objects.nonNull(checkInItemId)) {
      log.info("updateTransactionStatus:: Received checkIn event for itemId: {}", checkInItemId);
      systemUserScopedExecutionService.executeAsyncSystemUserScoped(tenantId, () ->
        transactionRepository.findTransactionByItemId(checkInItemId)
          .ifPresent(transactionEntity -> {
            switch (transactionEntity.getRole()) {
              case LENDER ->  lendingLibraryService.updateStatusByTransactionEntity(transactionEntity);
              default -> throw new IllegalArgumentException("Other roles are not implemented yet");
            }
          })
      );
    }
  }
}
