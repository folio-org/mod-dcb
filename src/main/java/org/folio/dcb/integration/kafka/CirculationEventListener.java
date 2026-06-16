package org.folio.dcb.integration.kafka;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;
import static org.folio.dcb.integration.kafka.TransactionHelper.getHeaderValue;
import static org.folio.dcb.utils.DcbConstants.CLOSED_LOAN_STATUS;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.integration.kafka.model.EventData;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class CirculationEventListener {

  public static final String CHECK_IN_LISTENER_ID = "mod-dcb-check-in-listener-id";
  public static final String CHECK_OUT_LOAN_LISTENER_ID = "mod-dcb-loan-listener-id";
  public static final String REQUEST_LISTENER_ID = "mod-dcb-request-listener-id";
  private static final String LOAN_EVENT_STATUS_UPDATE_MESSAGE =
    "{}:: status for event {} can not be updated with itemId {}";
  private final TransactionRepository transactionRepository;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;
  private final BaseLibraryService baseLibraryService;

  @KafkaListener(
    id = CHECK_OUT_LOAN_LISTENER_ID,
    topicPattern = "#{folioKafkaProperties.listener['loan'].topicPattern}",
    concurrency = "#{folioKafkaProperties.listener['loan'].concurrency}")
  public void handleLoanEvent(String data, MessageHeaders messageHeaders) {
    processMessage(data, messageHeaders, "circulation loan",
      TransactionHelper::parseLoanEvent,
      (tenantId, eventData) -> {
        if (eventData.isDcb()) {
          handleDcbLoanEvent(eventData, tenantId);
        } else {
          handleNonDcbLoanEvent(eventData, tenantId);
        }
      });
  }

  @KafkaListener(
    id = REQUEST_LISTENER_ID,
    topicPattern = "#{folioKafkaProperties.listener['request'].topicPattern}",
    concurrency = "#{folioKafkaProperties.listener['request'].concurrency}")
  public void handleRequestEvent(String data, MessageHeaders messageHeaders) {
    processMessage(data, messageHeaders, "request",
      TransactionHelper::parseRequestEvent, this::handleCirculationRequestEvent);
  }

  @KafkaListener(
    id = CHECK_IN_LISTENER_ID,
    topicPattern = "#{folioKafkaProperties.listener['check-in'].topicPattern}",
    concurrency = "#{folioKafkaProperties.listener['check-in'].concurrency}")
  public void handleCheckInEvent(String data, MessageHeaders messageHeaders) {
    processMessage(data, messageHeaders, "check-in",
      TransactionHelper::parseCheckInEvent,
      this::handleCirculationCheckInEvent);
  }

  public static void processMessage(String data, MessageHeaders messageHeaders, String type,
    Function<String, EventData> eventDataParser, BiConsumer<String, EventData> eventHandler) {

    var tenantHeaders = getHeaderValue(messageHeaders, XOkapiHeaders.TENANT, null);
    if (isEmpty(tenantHeaders)) {
      log.warn("processMessage:: tenantId is null, skipping processing: {}", type);
      return;
    }

    var eventData = eventDataParser.apply(data);
    if (eventData == null) {
      log.warn("processMessage:: parsed event data is null, skipping processing: {}", type);
      return;
    }

    eventHandler.accept(tenantHeaders.getFirst(), eventData);
  }

  private void handleDcbLoanEvent(EventData eventData, String tenantId) {
    log.debug("handleDcbLoanEvent:: eventType={}, itemId={}", eventData.getType(), eventData.getItemId());
    String itemId = eventData.getItemId();
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(tenantId, () ->
      transactionRepository.findTransactionByItemIdAndStatusNotInClosed(UUID.fromString(itemId))
        .ifPresent(transactionEntity -> processDcbTransactionEntity(eventData, transactionEntity))
    );
  }

  private void handleCirculationRequestEvent(String tenantId, EventData eventData) {
    log.debug("handleCirculationRequestEvent:: dcb flow for a request event");
    String requestId = eventData.getRequestId();
    if (requestId == null) {
      log.debug("handleCirculationRequestEvent:: requestId is null");
      return;
    }

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(tenantId, () ->
      transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(UUID.fromString(requestId))
        .ifPresent(transactionEntity -> processRequestEvent(transactionEntity, eventData))
    );
  }

  private void handleCirculationCheckInEvent(String tenantId, EventData eventData) {
    if (eventData.getCheckInServicePointId() == null) {
      return;
    }

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(tenantId, () -> {
      var itemUuid = UUID.fromString(eventData.getItemId());
      transactionRepository.findExpiredTransactionsByItemId(itemUuid).forEach(entity ->
        baseLibraryService.updateTransactionEntity(entity, TransactionStatus.StatusEnum.CLOSED));
    });
  }

  private void processDcbTransactionEntity(EventData event, TransactionEntity transactionEntity) {
    if (event.getType() == EventData.EventType.CHECK_OUT) {
      if (transactionEntity.getRole() == BORROWING_PICKUP || transactionEntity.getRole() == PICKUP) {
        baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
      }
    } else if (event.getType() == EventData.EventType.CHECK_IN) {
      if (transactionEntity.getRole() == LENDER) {
        baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.CLOSED);
      } else if (transactionEntity.getRole() == BORROWING_PICKUP || transactionEntity.getRole() == PICKUP) {
        baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
      }
    } else {
      logUnprocessedEvent("processDcbTransactionEntity", event);
    }
  }

  private void processRequestEvent(TransactionEntity transactionEntity, EventData eventData) {
    var type = eventData.getType();
    var role = transactionEntity.getRole();
    if (type == EventData.EventType.CANCEL && !eventData.isDcbReRequestCancellation()) {
      baseLibraryService.cancelTransactionEntity(transactionEntity);
    } else if (type == EventData.EventType.IN_TRANSIT && role == LENDER) {
      baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.OPEN);
    } else if (type == EventData.EventType.AWAITING_PICKUP && (role == BORROWING_PICKUP || role == PICKUP)) {
      baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.AWAITING_PICKUP);
    } else if (type == EventData.EventType.EXPIRED) {
      baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.EXPIRED);
    } else {
      log.info("processRequestEvent:: status for event {} can not be updated", eventData);
    }
  }

  private void handleNonDcbLoanEvent(EventData eventData, String tenantId) {
    log.debug("handleNonDcbLoanEvent:: eventType={}, itemId={}", eventData.getType(), eventData.getItemId());
    String itemId = eventData.getItemId();
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(tenantId, () ->
      transactionRepository.findSingleTransactionsByItemIdAndStatusNotInClosed(UUID.fromString(itemId))
        .ifPresent(transactionEntity -> processNonDcbTransactionEntity(eventData, transactionEntity))
    );
  }

  private void processNonDcbTransactionEntity(EventData event, TransactionEntity entity) {
    if (event.getType() == EventData.EventType.CHECK_OUT && isSelfBorrowingPickup(entity)) {
      baseLibraryService.updateTransactionEntity(entity, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    } else if (event.getType() == EventData.EventType.CHECK_IN && isSelfBorrowingPickupAndClosedLoan(entity, event)) {
      baseLibraryService.updateTransactionEntity(entity, TransactionStatus.StatusEnum.CLOSED);
    } else {
      logUnprocessedEvent("processNonDcbTransactionEntity", event);
    }
  }

  private static boolean isSelfBorrowingPickup(TransactionEntity entity) {
    return entity.getRole() == BORROWING_PICKUP && isTrue(entity.getSelfBorrowing());
  }

  private static boolean isSelfBorrowingPickupAndClosedLoan(TransactionEntity entity, EventData event) {
    return entity.getRole() == BORROWING_PICKUP
      && isTrue(entity.getSelfBorrowing())
      && CLOSED_LOAN_STATUS.equals(event.getLoanStatus());
  }

  private static void logUnprocessedEvent(String methodName, EventData event) {
    log.info(LOAN_EVENT_STATUS_UPDATE_MESSAGE, methodName, event.getType(), event.getItemId());
  }
}
