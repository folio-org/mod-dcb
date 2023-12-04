package org.folio.dcb.listener.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

import static org.folio.dcb.utils.TransactionHelper.getHeaderValue;
import static org.folio.dcb.utils.TransactionHelper.parseLoanEvent;
import static org.folio.dcb.utils.TransactionHelper.parseRequestEvent;

@Log4j2
@Component
@RequiredArgsConstructor
public class CirculationEventListener {
  public static final String CHECK_IN_LISTENER_ID = "mod-dcb-check-in-listener-id";
  public static final String CHECK_OUT_LOAN_LISTENER_ID = "mod-dcb-loan-listener-id";
  public static final String REQUEST_LISTENER_ID = "mod-dcb-request-listener-id";

  @Qualifier("lendingLibraryService")
  private final LibraryService lendingLibraryService;
  @Qualifier("borrowingPickupLibraryService")
  private final LibraryService borrowingLibraryService;
  @Qualifier("pickupLibraryService")
  private final LibraryService pickupLibraryService;
  private final TransactionRepository transactionRepository;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;
  private final BaseLibraryService baseLibraryService;

  @KafkaListener(
    id = CHECK_OUT_LOAN_LISTENER_ID,
    topicPattern = "#{folioKafkaProperties.listener['loan'].topicPattern}",
    concurrency = "#{folioKafkaProperties.listener['loan'].concurrency}")
  public void handleCheckOutEvent(String data, MessageHeaders messageHeaders) {
    String tenantId = getHeaderValue(messageHeaders, XOkapiHeaders.TENANT, null).get(0);
    var eventData = parseLoanEvent(data);
    if (Objects.nonNull(eventData) && eventData.getType() == EventData.EventType.CHECK_OUT) {
      String checkOutItemId = eventData.getItemId();
      if (Objects.nonNull(checkOutItemId)) {
        log.info("updateTransactionStatus:: Received checkOut event for itemId: {}", checkOutItemId);
        systemUserScopedExecutionService.executeAsyncSystemUserScoped(tenantId, () ->
          transactionRepository.findTransactionByItemIdAndStatusNotInClosed(UUID.fromString(checkOutItemId))
            .ifPresent(transactionEntity -> {
              switch (transactionEntity.getRole()) {
                case BORROWING_PICKUP -> borrowingLibraryService.updateStatusByTransactionEntity(transactionEntity);
                case PICKUP -> pickupLibraryService.updateStatusByTransactionEntity(transactionEntity);
                default -> throw new IllegalArgumentException("Other roles are not implemented yet");
              }
            })
        );
      }
    }
  }

  @KafkaListener(
    id = REQUEST_LISTENER_ID,
    topicPattern = "#{folioKafkaProperties.listener['request'].topicPattern}",
    concurrency = "#{folioKafkaProperties.listener['request'].concurrency}")
  public void handleRequestEvent(String data, MessageHeaders messageHeaders) {
    String tenantId = getHeaderValue(messageHeaders, XOkapiHeaders.TENANT, null).get(0);
    var eventData = parseRequestEvent(data);
    if (Objects.nonNull(eventData) && eventData.getType() == EventData.EventType.CANCEL) {
      String requestId = eventData.getRequestId();
      if (Objects.nonNull(requestId)) {
        log.info("updateTransactionStatus:: Received cancel event for requestId: {}", requestId);
        systemUserScopedExecutionService.executeAsyncSystemUserScoped(tenantId, () ->
          transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(UUID.fromString(requestId))
            .ifPresent(transactionEntity -> {
              if(eventData.getType() == EventData.EventType.CANCEL) {
                baseLibraryService.cancelTransactionEntity(transactionEntity);
              } else if(eventData.getType() == EventData.EventType.IN_TRANSIT){
                lendingLibraryService.updateStatusByTransactionEntity(transactionEntity);
              } else if(eventData.getType() == EventData.EventType.AWAITING_PICKUP) {
                switch (transactionEntity.getRole()) {
                  case BORROWING_PICKUP -> borrowingLibraryService.updateStatusByTransactionEntity(transactionEntity);
                  case PICKUP -> pickupLibraryService.updateStatusByTransactionEntity(transactionEntity);
                  default -> throw new IllegalArgumentException("Other roles are not implemented yet");
                }
              }
            })
        );
      }
    }
  }
}
