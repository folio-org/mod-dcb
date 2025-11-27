package org.folio.dcb.listener;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.domain.ResultList.asSinglePage;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.ItemStatus.NameEnum.AVAILABLE;
import static org.folio.dcb.domain.dto.ItemStatus.NameEnum.IN_TRANSIT;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CLOSED;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.EXPIRED;
import static org.folio.dcb.utils.CqlQuery.exactMatchById;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.ITEM_ID;
import static org.folio.dcb.utils.EntityUtils.getMockDataAsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.dcb.client.feign.InventoryItemStorageClient;
import org.folio.dcb.domain.dto.InventoryItem;
import org.folio.dcb.domain.dto.ItemStatus;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.folio.dcb.listener.kafka.CirculationEventListener;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CirculationCheckInEventListenerTest extends BaseTenantIntegrationTest {

  private static final String TENANT = "diku";
  private static final UUID ITEM_UUID = UUID.fromString(ITEM_ID);
  private static final String CHECK_IN_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/check_in.json");

  @Captor private ArgumentCaptor<TransactionEntity> transactionEntityArgumentCaptor;
  @Autowired private CirculationEventListener eventListener;
  @MockitoBean private TransactionRepository repository;
  @MockitoBean private InventoryItemStorageClient itemClient;

  @Test
  void handleCheckInEvent_positive_expiredDcbTransactionFound() {
    var item = new InventoryItem().id(ITEM_ID).status(new ItemStatus().name(AVAILABLE));

    when(repository.findExpiredLenderTransactionsByItemId(ITEM_UUID)).thenReturn(List.of(transactionEntity()));
    when(repository.save(transactionEntityArgumentCaptor.capture())).then(v -> v.getArgument(0));
    when(itemClient.fetchItemByQuery(exactMatchById(ITEM_ID))).thenReturn(asSinglePage(item));

    eventListener.handleCheckInEvent(CHECK_IN_EVENT_SAMPLE, messageHeaders());

    verify(repository).save(any());
    verify(itemClient).fetchItemByQuery(exactMatchById(ITEM_ID));
    var savedValue = transactionEntityArgumentCaptor.getValue();
    assertThat(savedValue.getStatus()).isEqualTo(CLOSED);
  }

  @Test
  void handleCheckInEvent_positive_expiredDcbTransactionFoundWithNotAvailableItem() {
    var item = new InventoryItem().id(ITEM_ID).status(new ItemStatus().name(IN_TRANSIT));

    when(repository.findExpiredLenderTransactionsByItemId(ITEM_UUID)).thenReturn(List.of(transactionEntity()));
    when(repository.save(transactionEntityArgumentCaptor.capture())).then(v -> v.getArgument(0));
    when(itemClient.fetchItemByQuery(exactMatchById(ITEM_ID))).thenReturn(asSinglePage(item));

    eventListener.handleCheckInEvent(CHECK_IN_EVENT_SAMPLE, messageHeaders());

    verify(repository, never()).save(any());
    verify(itemClient).fetchItemByQuery(exactMatchById(ITEM_ID));
  }

  @Test
  void handleCheckInEvent_positive_expiredDcbTransactionNotFound() {
    var item = new InventoryItem().id(ITEM_ID).status(new ItemStatus().name(AVAILABLE));

    when(repository.findExpiredLenderTransactionsByItemId(ITEM_UUID)).thenReturn(emptyList());
    when(itemClient.fetchItemByQuery(exactMatchById(ITEM_ID))).thenReturn(asSinglePage(item));

    eventListener.handleCheckInEvent(CHECK_IN_EVENT_SAMPLE, messageHeaders());

    verify(repository, never()).save(any());
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
