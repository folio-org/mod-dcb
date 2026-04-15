package org.folio.dcb.listener;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.EXPIRED;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.ITEM_ID;
import static org.folio.dcb.utils.EntityUtils.getMockDataAsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.folio.dcb.integration.kafka.CirculationEventListener;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ExtendWith(MockitoExtension.class)
class CirculationCheckInEventListenerIT extends BaseTenantIntegrationTest {

  private static final String TENANT = "diku";
  private static final UUID ITEM_UUID = UUID.fromString(ITEM_ID);
  private static final String CHECK_IN_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/check_in.json");

  @Captor private ArgumentCaptor<TransactionEntity> transactionEntityArgumentCaptor;
  @Autowired private CirculationEventListener eventListener;
  @MockitoBean private TransactionRepository repository;

  @Test
  void handleCheckInEvent_positive_expiredDcbTransactionFound() {
    when(repository.findExpiredTransactionsByItemId(ITEM_UUID)).thenReturn(List.of(transactionEntity()));
    when(repository.save(transactionEntityArgumentCaptor.capture())).then(v -> v.getArgument(0));

    eventListener.handleCheckInEvent(CHECK_IN_EVENT_SAMPLE, messageHeaders());

    verify(repository).save(any());
    assertThat(transactionEntityArgumentCaptor.getValue().getStatus()).isEqualTo(CLOSED);
  }

  @Test
  void handleCheckInEvent_positive_expiredDcbTransactionNotFound() {
    when(repository.findExpiredTransactionsByItemId(ITEM_UUID)).thenReturn(emptyList());

    eventListener.handleCheckInEvent(CHECK_IN_EVENT_SAMPLE, messageHeaders());

    verify(repository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "",
    "{\"type\":\"CREATED\"}",
    "{\"type\":\"UPDATED\"}",
    "{\"type\":\"CREATED\",\"data\":{\"new\":{\"itemId\":\"%s\"}}}",
  })
  void handleCheckInEvent_parameterized_ignoredPayload(String payload) {
    eventListener.handleCheckInEvent(payload, messageHeaders());
    verifyNoInteractions(repository);
  }

  @Test
  void handleCheckInEvent_parameterized_emptyMessageHeaders() {
    eventListener.handleCheckInEvent(CHECK_IN_EVENT_SAMPLE, new MessageHeaders(emptyMap()));
    verifyNoInteractions(repository);
  }

  private static MessageHeaders messageHeaders() {
    return new MessageHeaders(Map.of(XOkapiHeaders.TENANT, TENANT.getBytes()));
  }

  private static TransactionEntity transactionEntity() {
    return TransactionEntity.builder()
      .id(DCB_TRANSACTION_ID)
      .itemId(ITEM_ID)
      .status(EXPIRED)
      .role(LENDER)
      .requestId(UUID.fromString(EXISTED_PATRON_ID))
      .build();
  }
}

