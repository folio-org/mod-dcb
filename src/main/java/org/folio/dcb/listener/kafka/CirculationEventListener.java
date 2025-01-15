package org.folio.dcb.listener.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;
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

  private final TransactionRepository transactionRepository;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;
  private final BaseLibraryService baseLibraryService;

  @KafkaListener(
    id = CHECK_OUT_LOAN_LISTENER_ID,
    topicPattern = "#{folioKafkaProperties.listener['loan'].topicPattern}",
    concurrency = "#{folioKafkaProperties.listener['loan'].concurrency}")
  public void handleLoanEvent(String data, MessageHeaders messageHeaders) {
    String tenantId = getHeaderValue(messageHeaders, XOkapiHeaders.TENANT, null).get(0);
    var eventData = parseLoanEvent(data);
    if (Objects.nonNull(eventData)) {
      String itemId = eventData.getItemId();
      systemUserScopedExecutionService.executeAsyncSystemUserScoped(tenantId, () ->
        transactionRepository.findTransactionByItemIdAndStatusNotInClosed(UUID.fromString(itemId))
          .ifPresent(transactionEntity -> {
            if(eventData.getType() == EventData.EventType.CHECK_OUT) {
              if(transactionEntity.getRole() == BORROWING_PICKUP || transactionEntity.getRole() == PICKUP) {
                baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
              }
            } else if(eventData.getType() == EventData.EventType.CHECK_IN) {
              if(transactionEntity.getRole() == LENDER) {
                baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.CLOSED);
              } else if(transactionEntity.getRole() == BORROWING_PICKUP || transactionEntity.getRole() == PICKUP) {
                baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
              }
            } else {
              log.info("handleLoanEvent:: status for event {} can not be updated", eventData.getType());
            }
          })
      );
    }
  }

  @KafkaListener(
    id = REQUEST_LISTENER_ID,
    topicPattern = "#{folioKafkaProperties.listener['request'].topicPattern}",
    concurrency = "#{folioKafkaProperties.listener['request'].concurrency}")
  public void handleRequestEvent(String data, MessageHeaders messageHeaders) {
    String tenantId = getHeaderValue(messageHeaders, XOkapiHeaders.TENANT, null).get(0);
    var eventData = parseRequestEvent(data);
    if (Objects.nonNull(eventData)) {
      String requestId = eventData.getRequestId();
      if (Objects.nonNull(requestId)) {
        systemUserScopedExecutionService.executeAsyncSystemUserScoped(tenantId, () ->
          transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(UUID.fromString(requestId))
            .ifPresent(transactionEntity -> {
              if(eventData.getType() == EventData.EventType.CANCEL && !eventData.isDcbReRequestCancellation()) {
                baseLibraryService.cancelTransactionEntity(transactionEntity);
              } else if(eventData.getType() == EventData.EventType.IN_TRANSIT && transactionEntity.getRole() == LENDER) {
                baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.OPEN);
              } else if(eventData.getType() == EventData.EventType.AWAITING_PICKUP && (transactionEntity.getRole() == BORROWING_PICKUP || transactionEntity.getRole() == PICKUP)) {
                baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.AWAITING_PICKUP);
              } else {
                log.info("handleRequestEvent:: status for event {} can not be updated", eventData);
              }
            })
        );
      }
    }
  }
}
