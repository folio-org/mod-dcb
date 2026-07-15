package org.folio.dcb.integration.kafka;

import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.folio.dcb.domain.dto.DcbTransaction.RoleEnum;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.messaging.MessageHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.folio.spring.integration.XOkapiHeaders;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import org.folio.dcb.integration.kafka.CirculationEventListener;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(MockitoExtension.class)
class CirculationEventListenerTest {

  @InjectMocks
  private CirculationEventListener circulationEventListener;

  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private SystemUserScopedExecutionService systemUserScopedExecutionService;

  @Mock
  private BaseLibraryService baseLibraryService;

    private static Stream<Arguments> roleAndExpectedUpdateCount() {
    return Stream.of(
      Arguments.of(RoleEnum.BORROWING_PICKUP, 1),
      Arguments.of(RoleEnum.PICKUP, 1),
      Arguments.of(RoleEnum.LENDER, 0)
    );
  }

    private static Stream<Arguments> roleAndExpectedCheckInStatus() {
    return Stream.of(
      Arguments.of(RoleEnum.LENDER, TransactionStatus.StatusEnum.CLOSED),
      Arguments.of(RoleEnum.PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_IN),
      Arguments.of(RoleEnum.BORROWING_PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_IN)
    );
  }

    private static Stream<Arguments> selfBorrowingScenarios() {
    return Stream.of(
      Arguments.of(BORROWING_PICKUP, true, true),
      Arguments.of(BORROWING_PICKUP, false, false),
      Arguments.of(LENDER, true, false)
    );
  }

    private static Stream<Arguments> selfBorrowingCheckInScenarios() {
  return Stream.of(
    Arguments.of("Closed", true),
    Arguments.of("Open", false)
  );
}

    @ParameterizedTest
  @MethodSource("roleAndExpectedUpdateCount")
  void handleDcbLoanCheckOutEventTest(RoleEnum role, int expectedUpdateCount) {
    // TestMate-f25766a066b7a7555242da735425fc7a
    // Given
    String tenantId = "diku";
    String itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    String payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "action": "checkedout",
            "isDcb": true
          }
        }
      }
      """.formatted(itemId);
    MessageHeaders messageHeaders = new MessageHeaders(Map.of(
      XOkapiHeaders.TENANT, tenantId.getBytes(StandardCharsets.UTF_8)
    ));
    TransactionEntity transactionEntity = TransactionEntity.builder()
      .itemId(itemId)
      .role(role)
      .status(TransactionStatus.StatusEnum.OPEN)
      .build();
    doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(1);
      runnable.run();
      return null;
    }).when(systemUserScopedExecutionService).executeAsyncSystemUserScoped(eq(tenantId), any(Runnable.class));
    when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(UUID.fromString(itemId)))
      .thenReturn(Optional.of(transactionEntity));
    // When
    circulationEventListener.handleLoanEvent(payload, messageHeaders);
    // Then
    verify(transactionRepository).findTransactionByItemIdAndStatusNotInClosed(UUID.fromString(itemId));
    if (expectedUpdateCount > 0) {
      verify(baseLibraryService).updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    } else {
      verify(baseLibraryService, never()).updateTransactionEntity(any(), any());
    }
  }

    @ParameterizedTest
  @MethodSource("roleAndExpectedCheckInStatus")
  void handleDcbLoanCheckInEventTest(RoleEnum role, TransactionStatus.StatusEnum expectedStatus) {
    // TestMate-1af11fa062b0ceb09d9d307311ecc386
    // Given
    String tenantId = "diku";
    String itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    String payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "action": "checkedin",
            "isDcb": true
          }
        }
      }
      """.formatted(itemId);
    MessageHeaders messageHeaders = new MessageHeaders(Map.of(
      XOkapiHeaders.TENANT, tenantId.getBytes(StandardCharsets.UTF_8)
    ));
    TransactionEntity transactionEntity = TransactionEntity.builder()
      .itemId(itemId)
      .role(role)
      .status(TransactionStatus.StatusEnum.OPEN)
      .build();
    doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(1);
      runnable.run();
      return null;
    }).when(systemUserScopedExecutionService).executeAsyncSystemUserScoped(eq(tenantId), any(Runnable.class));
    when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(UUID.fromString(itemId)))
      .thenReturn(Optional.of(transactionEntity));
    // When
    circulationEventListener.handleLoanEvent(payload, messageHeaders);
    // Then
    verify(transactionRepository).findTransactionByItemIdAndStatusNotInClosed(UUID.fromString(itemId));
    verify(baseLibraryService).updateTransactionEntity(transactionEntity, expectedStatus);
  }

    @ParameterizedTest
  @MethodSource("selfBorrowingScenarios")
  void handleNonDcbLoanCheckOutSelfBorrowingTest(RoleEnum role, boolean selfBorrowing, boolean shouldUpdate) {
    // TestMate-e2ee656c46a0dad615d8f602e178a5c5
    // Given
    String tenantId = "diku";
    String itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    String payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "action": "checkedout",
            "isDcb": false
          }
        }
      }
      """.formatted(itemId);
    MessageHeaders messageHeaders = new MessageHeaders(Map.of(
      XOkapiHeaders.TENANT, tenantId.getBytes(StandardCharsets.UTF_8)
    ));
    TransactionEntity transactionEntity = TransactionEntity.builder()
      .itemId(itemId)
      .role(role)
      .selfBorrowing(selfBorrowing)
      .status(TransactionStatus.StatusEnum.OPEN)
      .build();
    doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(1);
      runnable.run();
      return null;
    }).when(systemUserScopedExecutionService).executeAsyncSystemUserScoped(eq(tenantId), any(Runnable.class));
    when(transactionRepository.findSingleTransactionsByItemIdAndStatusNotInClosed(UUID.fromString(itemId)))
      .thenReturn(Optional.of(transactionEntity));
    // When
    circulationEventListener.handleLoanEvent(payload, messageHeaders);
    // Then
    verify(transactionRepository).findSingleTransactionsByItemIdAndStatusNotInClosed(UUID.fromString(itemId));
    if (shouldUpdate) {
      verify(baseLibraryService).updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    } else {
      verify(baseLibraryService, never()).updateTransactionEntity(any(), any());
    }
  }

    @ParameterizedTest
@MethodSource("selfBorrowingCheckInScenarios")
void handleNonDcbLoanCheckInSelfBorrowingTest(String eventLoanStatus, boolean shouldUpdate) {
  // TestMate-34641872294c85ba1b5d8ff881c954fa
  // Given
  String tenantId = "diku";
  String itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
  String payload = """
    {
      "type": "UPDATED",
      "data": {
        "new": {
          "itemId": "%s",
          "action": "checkedin",
          "isDcb": false,
          "status": {
            "name": "%s"
          }
        }
      }
    }
    """.formatted(itemId, eventLoanStatus);
  MessageHeaders messageHeaders = new MessageHeaders(Map.of(
    XOkapiHeaders.TENANT, tenantId.getBytes(StandardCharsets.UTF_8)
  ));
  TransactionEntity transactionEntity = TransactionEntity.builder()
    .itemId(itemId)
    .role(RoleEnum.BORROWING_PICKUP)
    .selfBorrowing(true)
    .status(TransactionStatus.StatusEnum.OPEN)
    .build();
  doAnswer(invocation -> {
    Runnable runnable = invocation.getArgument(1);
    runnable.run();
    return null;
  }).when(systemUserScopedExecutionService).executeAsyncSystemUserScoped(eq(tenantId), any(Runnable.class));
  when(transactionRepository.findSingleTransactionsByItemIdAndStatusNotInClosed(UUID.fromString(itemId)))
    .thenReturn(Optional.of(transactionEntity));
  // When
  circulationEventListener.handleLoanEvent(payload, messageHeaders);
  // Then
  verify(transactionRepository).findSingleTransactionsByItemIdAndStatusNotInClosed(UUID.fromString(itemId));
  if (shouldUpdate) {
    verify(baseLibraryService).updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.CLOSED);
  } else {
    verify(baseLibraryService, never()).updateTransactionEntity(any(), any());
  }
}

    @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void handleLoanEventWhenTransactionNotFoundTest(boolean isDcb) {
    // TestMate-c1139da62a64caa3ed1f26bbc783a5b3
    // Given
    String tenantId = "diku";
    String itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    String payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "action": "checkedout",
            "isDcb": %b
          }
        }
      }
      """.formatted(itemId, isDcb);
    MessageHeaders messageHeaders = new MessageHeaders(Map.of(
      XOkapiHeaders.TENANT, tenantId.getBytes(StandardCharsets.UTF_8)
    ));
    doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(1);
      runnable.run();
      return null;
    }).when(systemUserScopedExecutionService).executeAsyncSystemUserScoped(eq(tenantId), any(Runnable.class));
    if (isDcb) {
      when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(UUID.fromString(itemId)))
        .thenReturn(Optional.empty());
    } else {
      when(transactionRepository.findSingleTransactionsByItemIdAndStatusNotInClosed(UUID.fromString(itemId)))
        .thenReturn(Optional.empty());
    }
    // When
    circulationEventListener.handleLoanEvent(payload, messageHeaders);
    // Then
    if (isDcb) {
      verify(transactionRepository).findTransactionByItemIdAndStatusNotInClosed(UUID.fromString(itemId));
    } else {
      verify(transactionRepository).findSingleTransactionsByItemIdAndStatusNotInClosed(UUID.fromString(itemId));
    }
    verify(baseLibraryService, never()).updateTransactionEntity(any(), any());
  }

}
