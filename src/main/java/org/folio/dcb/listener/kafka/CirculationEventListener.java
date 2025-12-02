package org.folio.dcb.listener.kafka;

import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.folio.dcb.service.impl.LendingLibraryServiceImpl;
import org.folio.dcb.utils.TransactionHelper;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;
import static org.folio.dcb.utils.DCBConstants.CLOSED_LOAN_STATUS;
import static org.folio.dcb.utils.TransactionHelper.getHeaderValue;

@Log4j2
@Component
@RequiredArgsConstructor
public class CirculationEventListener {
  public static final String CHECK_IN_LISTENER_ID = "mod-dcb-check-in-listener-id";
  public static final String CHECK_OUT_LOAN_LISTENER_ID = "mod-dcb-loan-listener-id";
  public static final String REQUEST_LISTENER_ID = "mod-dcb-request-listener-id";
  private static final String LOAN_EVENT_STATUS_UPDATE_MESSAGE = "{}:: status for event {} can not be updated with itemId {}";
  private final TransactionRepository transactionRepository;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;
  private final BaseLibraryService baseLibraryService;

  @Qualifier("lendingLibraryService")
  private final LibraryService lendingLibraryService;

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
        .ifPresent(transactionEntity -> handleRequestEvent(transactionEntity, eventData))
    );
  }

  private void handleCirculationCheckInEvent(String tenantId, EventData eventData) {
    if (eventData.getCheckInServicePointId() == null) {
      return;
    }

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(tenantId, () -> {
      var itemUuid = UUID.fromString(eventData.getItemId());
      transactionRepository.findExpiredLenderTransactionsByItemId(itemUuid).forEach(entity ->
        ((LendingLibraryServiceImpl) lendingLibraryService)
          .closeExpiredTransactionEntity(entity, eventData.getCheckInServicePointId()));
    });
  }

  private void processDcbTransactionEntity(EventData eventData, TransactionEntity transactionEntity) {
    if (eventData.getType() == EventData.EventType.CHECK_OUT) {
      if (transactionEntity.getRole() == BORROWING_PICKUP || transactionEntity.getRole() == PICKUP) {
        baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
      }
    } else if (eventData.getType() == EventData.EventType.CHECK_IN) {
      if (transactionEntity.getRole() == LENDER) {
        baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.CLOSED);
      } else if (transactionEntity.getRole() == BORROWING_PICKUP || transactionEntity.getRole() == PICKUP) {
        baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
      }
    } else {
      log.info(LOAN_EVENT_STATUS_UPDATE_MESSAGE, "processDcbTransactionEntity",  eventData.getType(), eventData.getItemId());
    }
  }

  private void handleRequestEvent(TransactionEntity transactionEntity, EventData eventData) {
    var type = eventData.getType();
    var role = transactionEntity.getRole();
    if (type == EventData.EventType.CANCEL && !eventData.isDcbReRequestCancellation()) {
      baseLibraryService.cancelTransactionEntity(transactionEntity);
    } else if (type == EventData.EventType.IN_TRANSIT && role == LENDER) {
      baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.OPEN);
    } else if (type == EventData.EventType.AWAITING_PICKUP && (role == BORROWING_PICKUP || role == PICKUP)) {
      baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.AWAITING_PICKUP);
    } else if (type == EventData.EventType.EXPIRED && role == LENDER) {
      baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.EXPIRED);
    } else {
      log.info("handleRequestEvent:: status for event {} can not be updated", eventData);
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

  private void processNonDcbTransactionEntity(EventData eventData, TransactionEntity transactionEntity) {
    if (eventData.getType() == EventData.EventType.CHECK_OUT && isSelfBorrowingPickup(transactionEntity)) {
      baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    } else if (eventData.getType() == EventData.EventType.CHECK_IN &&
               isSelfBorrowingPickupAndClosedLoan(transactionEntity, eventData)) {
      baseLibraryService.updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.CLOSED);
    } else {
      log.info(LOAN_EVENT_STATUS_UPDATE_MESSAGE, "processNonDcbTransactionEntity", eventData.getType(), eventData.getItemId());
    }
  }

  private static boolean isSelfBorrowingPickup(TransactionEntity transactionEntity) {
    return transactionEntity.getRole() == BORROWING_PICKUP && BooleanUtils.isTrue(transactionEntity.getSelfBorrowing());
  }

  private static boolean isSelfBorrowingPickupAndClosedLoan(TransactionEntity transactionEntity,
    EventData eventData) {
    return transactionEntity.getRole() == BORROWING_PICKUP && BooleanUtils.isTrue(transactionEntity.getSelfBorrowing())
           && CLOSED_LOAN_STATUS.equals(eventData.getLoanStatus());
  }
}
