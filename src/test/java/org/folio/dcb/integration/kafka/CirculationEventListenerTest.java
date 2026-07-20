package org.folio.dcb.integration.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_OUT;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.dcb.domain.dto.DcbTransaction.RoleEnum;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.messaging.MessageHeaders;

@ExtendWith(MockitoExtension.class)
class CirculationEventListenerTest {

  private static final UUID ITEM_UUID = UUID.randomUUID();
  private static final String ITEM_ID = ITEM_UUID.toString();
  private static final String TENANT_ID = "testtenant";

  @InjectMocks private CirculationEventListener circulationEventListener;
  @Mock private TransactionRepository transactionRepository;
  @Mock private SystemUserScopedExecutionService systemUserScopedExecutionService;
  @Mock private BaseLibraryService baseLibraryService;

  @ParameterizedTest
  @MethodSource("checkoutRolesThatShouldUpdate")
  void handleDcbLoanCheckOutEventTest_shouldUpdate(RoleEnum role) {
    var payload = eventPayload("checkedout", true, "OPEN");
    var transactionEntity = transactionEntity(role, false);

    doAnswer(invokeRunnable()).when(systemUserScopedExecutionService)
      .executeAsyncSystemUserScoped(eq(TENANT_ID), any(Runnable.class));
    when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(ITEM_UUID))
      .thenReturn(Optional.of(transactionEntity));

    circulationEventListener.handleLoanEvent(payload, messageHeaders());

    verify(transactionRepository).findTransactionByItemIdAndStatusNotInClosed(ITEM_UUID);
    verify(baseLibraryService).updateTransactionEntity(transactionEntity, ITEM_CHECKED_OUT);
  }

  @Test
  void handleDcbLoanCheckOutEventTest_shouldNotUpdate() {
    var payload = eventPayload("checkedout", true, "OPEN");
    var transactionEntity = transactionEntity(RoleEnum.LENDER, false);

    doAnswer(invokeRunnable()).when(systemUserScopedExecutionService)
      .executeAsyncSystemUserScoped(eq(TENANT_ID), any(Runnable.class));
    when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(ITEM_UUID))
      .thenReturn(Optional.of(transactionEntity));

    circulationEventListener.handleLoanEvent(payload, messageHeaders());

    verify(transactionRepository)
      .findTransactionByItemIdAndStatusNotInClosed(ITEM_UUID);
    verify(baseLibraryService, never()).updateTransactionEntity(any(), any());
  }

  @ParameterizedTest
  @MethodSource("roleAndExpectedCheckInStatus")
  void handleDcbLoanCheckInEventTest(RoleEnum role, TransactionStatus.StatusEnum expectedStatus) {
    // TestMate-1af11fa062b0ceb09d9d307311ecc386
    var payload = eventPayload("checkedin", true, "OPEN");
    var transactionEntity = transactionEntity(role, false);

    doAnswer(invokeRunnable()).when(systemUserScopedExecutionService)
      .executeAsyncSystemUserScoped(eq(TENANT_ID), any(Runnable.class));
    when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(ITEM_UUID))
      .thenReturn(Optional.of(transactionEntity));

    circulationEventListener.handleLoanEvent(payload, messageHeaders());

    verify(transactionRepository).findTransactionByItemIdAndStatusNotInClosed(ITEM_UUID);
    verify(baseLibraryService).updateTransactionEntity(transactionEntity, expectedStatus);
  }

  @Test
  void handleNonDcbLoanCheckOutSelfBorrowingTest_shouldUpdate() {
    var payload = eventPayload("checkedout", false, "OPEN");
    var transactionEntity = transactionEntity(BORROWING_PICKUP, true);

    doAnswer(invokeRunnable()).when(systemUserScopedExecutionService)
      .executeAsyncSystemUserScoped(eq(TENANT_ID), any(Runnable.class));
    when(transactionRepository.findSingleTransactionsByItemIdAndStatusNotInClosed(ITEM_UUID))
      .thenReturn(Optional.of(transactionEntity));

    circulationEventListener.handleLoanEvent(payload, messageHeaders());

    verify(transactionRepository).findSingleTransactionsByItemIdAndStatusNotInClosed(ITEM_UUID);
    verify(baseLibraryService).updateTransactionEntity(transactionEntity, ITEM_CHECKED_OUT);
  }

  @ParameterizedTest
  @MethodSource("selfBorrowingCheckoutNoUpdateScenarios")
  void handleLoanEvent_parameterized_selfBorrowingShouldNotUpdate(RoleEnum role, boolean selfBorrowing) {
    var payload = eventPayload("checkedout", false, "OPEN");
    var transactionEntity = transactionEntity(role, selfBorrowing);

    doAnswer(invokeRunnable()).when(systemUserScopedExecutionService)
      .executeAsyncSystemUserScoped(eq(TENANT_ID), any(Runnable.class));
    when(transactionRepository.findSingleTransactionsByItemIdAndStatusNotInClosed(ITEM_UUID))
      .thenReturn(Optional.of(transactionEntity));

    circulationEventListener.handleLoanEvent(payload, messageHeaders());

    verify(transactionRepository).findSingleTransactionsByItemIdAndStatusNotInClosed(ITEM_UUID);
    verify(baseLibraryService, never()).updateTransactionEntity(any(), any());
  }

  @Test
  void handleLoanEvent_positive_shouldUpdateForBorrowingPickupAndSelfBorrowing() {
    // TestMate-34641872294c85ba1b5d8ff881c954fa - Closed loan status should update to CLOSED
    var payload = eventPayload("checkedin", false, "Closed");
    var transactionEntity = transactionEntity(BORROWING_PICKUP, true);

    doAnswer(invokeRunnable()).when(systemUserScopedExecutionService)
      .executeAsyncSystemUserScoped(eq(TENANT_ID), any(Runnable.class));
    when(transactionRepository.findSingleTransactionsByItemIdAndStatusNotInClosed(ITEM_UUID))
      .thenReturn(Optional.of(transactionEntity));

    circulationEventListener.handleLoanEvent(payload, messageHeaders());

    verify(transactionRepository).findSingleTransactionsByItemIdAndStatusNotInClosed(ITEM_UUID);
    verify(baseLibraryService).updateTransactionEntity(transactionEntity, TransactionStatus.StatusEnum.CLOSED);
  }

  @Test
  void handleNonDcbLoanCheckInSelfBorrowingTest_shouldNotUpdate() {
    // TestMate-34641872294c85ba1b5d8ff881c954fa - Open status should not update
    var payload = eventPayload("checkedin", false, "OPEN");
    var transactionEntity = transactionEntity(BORROWING_PICKUP, true);

    doAnswer(invokeRunnable()).when(systemUserScopedExecutionService)
      .executeAsyncSystemUserScoped(eq(TENANT_ID), any(Runnable.class));
    when(transactionRepository.findSingleTransactionsByItemIdAndStatusNotInClosed(ITEM_UUID))
      .thenReturn(Optional.of(transactionEntity));

    circulationEventListener.handleLoanEvent(payload, messageHeaders());

    verify(transactionRepository).findSingleTransactionsByItemIdAndStatusNotInClosed(ITEM_UUID);
    verify(baseLibraryService, never()).updateTransactionEntity(any(), any());
  }

  @Test
  void handleDcbLoanEventWhenTransactionNotFoundTest() {
    // TestMate-c1139da62a64caa3ed1f26bbc783a5b3 - DCB transaction
    var payload = eventPayload("checkedin", true, "OPEN");

    doAnswer(invokeRunnable()).when(systemUserScopedExecutionService)
      .executeAsyncSystemUserScoped(eq(TENANT_ID), any(Runnable.class));
    when(transactionRepository.findTransactionByItemIdAndStatusNotInClosed(ITEM_UUID))
      .thenReturn(Optional.empty());

    circulationEventListener.handleLoanEvent(payload, messageHeaders());

    verify(transactionRepository).findTransactionByItemIdAndStatusNotInClosed(ITEM_UUID);
    verify(baseLibraryService, never()).updateTransactionEntity(any(), any());
  }

  @Test
  void handleNonDcbLoanEventWhenTransactionNotFoundTest() {
    // TestMate-c1139da62a64caa3ed1f26bbc783a5b3 - Non-DCB transaction
    var payload = eventPayload("checkedin", false, "OPEN");

    doAnswer(invokeRunnable()).when(systemUserScopedExecutionService)
      .executeAsyncSystemUserScoped(eq(TENANT_ID), any(Runnable.class));
    when(transactionRepository.findSingleTransactionsByItemIdAndStatusNotInClosed(ITEM_UUID))
      .thenReturn(Optional.empty());

    circulationEventListener.handleLoanEvent(payload, messageHeaders());

    verify(transactionRepository).findSingleTransactionsByItemIdAndStatusNotInClosed(ITEM_UUID);
    verify(baseLibraryService, never()).updateTransactionEntity(any(), any());
  }

  private static Stream<Arguments> selfBorrowingCheckoutNoUpdateScenarios() {
    return Stream.of(
      Arguments.of(BORROWING_PICKUP, false),
      Arguments.of(LENDER, true)
    );
  }

  private static Stream<Arguments> checkoutRolesThatShouldUpdate() {
    return Stream.of(
      Arguments.of(RoleEnum.BORROWING_PICKUP),
      Arguments.of(RoleEnum.PICKUP)
    );
  }

  private static Stream<Arguments> roleAndExpectedCheckInStatus() {
    return Stream.of(
      Arguments.of(RoleEnum.LENDER, TransactionStatus.StatusEnum.CLOSED),
      Arguments.of(RoleEnum.PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_IN),
      Arguments.of(RoleEnum.BORROWING_PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_IN)
    );
  }

  private static TransactionEntity transactionEntity(RoleEnum role, boolean selfBorrowing) {
    return TransactionEntity.builder()
      .itemId(ITEM_ID)
      .role(role)
      .status(TransactionStatus.StatusEnum.OPEN)
      .selfBorrowing(selfBorrowing)
      .build();
  }

  private static MessageHeaders messageHeaders() {
    return new MessageHeaders(Map.of(TENANT, TENANT_ID.getBytes(UTF_8)));
  }

  private static String eventPayload(String action, boolean isDcb, String status) {
    return """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "action": "%s",
            "isDcb": %s,
            "status": {
              "name": "%s"
            }
          }
        }
      }
      """.formatted(ITEM_ID, action, isDcb, status);
  }

  private static Answer<Void> invokeRunnable() {
    return invocation -> {
      Runnable runnable = invocation.getArgument(1);
      runnable.run();
      return null;
    };
  }
}
