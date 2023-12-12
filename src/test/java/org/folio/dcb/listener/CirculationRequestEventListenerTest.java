package org.folio.dcb.listener;

import org.folio.dcb.controller.BaseIT;
import org.folio.dcb.domain.dto.ItemStatus;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.listener.kafka.CirculationEventListener;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.impl.BaseLibraryService;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.*;
import static org.folio.dcb.utils.EntityUtils.createCirculationItem;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.getMockDataAsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class CirculationRequestEventListenerTest extends BaseIT {

  private static final String CHECK_IN_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/check_in.json");
  private static final String CHECK_IN_TRANSIT_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/check_in_transit.json");
  private static final String CHECK_IN_UNDEFINED_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/request_undefined.json");
  private static final String REQUEST_CANCEL_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/cancel_request.json");

  @InjectMocks
  private BaseLibraryService baseLibraryService;

  @Autowired
  private CirculationEventListener eventListener ;

  @MockBean
  private TransactionRepository transactionRepository;

  @MockBean
  private CirculationItemService circulationItemService;

  @Test
  void handleCheckInEventInPickupFromOpenToAwaitingPickupTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("5b95877d-86c0-4cb7-a0cd-7660b348ae5d");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.OPEN);
    transactionEntity.setRole(PICKUP);

    var circulationItem = createCirculationItem();
    circulationItem.setStatus(org.folio.dcb.domain.dto.ItemStatus.builder().name(org.folio.dcb.domain.dto.ItemStatus.NameEnum.AWAITING_PICKUP).build());

    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    when(circulationItemService.fetchItemById(anyString())).thenReturn(circulationItem);
    eventListener.handleRequestEvent(CHECK_IN_EVENT_SAMPLE, messageHeaders);
    Mockito.verify(transactionRepository).save(any());
  }

  @Test
  void handleCheckInEventInBorrowingFromOpenToAwaitingPickup() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("5b95877d-86c0-4cb7-a0cd-7660b348ae5d");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.OPEN);
    transactionEntity.setRole(BORROWING_PICKUP);

    var circulationItem = createCirculationItem();
    circulationItem.setStatus(org.folio.dcb.domain.dto.ItemStatus.builder().name(org.folio.dcb.domain.dto.ItemStatus.NameEnum.AWAITING_PICKUP).build());

    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    when(circulationItemService.fetchItemById(anyString())).thenReturn(circulationItem);
    eventListener.handleRequestEvent(CHECK_IN_EVENT_SAMPLE, messageHeaders);
    Mockito.verify(transactionRepository).save(any());
  }

  @Test
  void handleCancelRequestTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setRole(LENDER);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleRequestEvent(REQUEST_CANCEL_EVENT_SAMPLE, messageHeaders);
    Mockito.verify(transactionRepository).save(any());
  }

  @Test
  void handleOpenRequestTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("5b95877e-86c0-4cb7-a0cd-7660b348ae5d");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(LENDER);

    var circulationItem = createCirculationItem();
    circulationItem.setStatus(org.folio.dcb.domain.dto.ItemStatus.builder().name(ItemStatus.NameEnum.IN_TRANSIT).build());

    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    when(circulationItemService.fetchItemById(anyString())).thenReturn(circulationItem);
    eventListener.handleRequestEvent(CHECK_IN_TRANSIT_EVENT_SAMPLE, messageHeaders);
    Mockito.verify(transactionRepository).save(any());
  }

  @Test
  void handleUndefinedEventTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("5b95877e-86c0-4cb7-a0cd-7660b348ae5d");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(LENDER);

    var circulationItem = createCirculationItem();
    circulationItem.setStatus(org.folio.dcb.domain.dto.ItemStatus.builder().name(ItemStatus.NameEnum.IN_TRANSIT).build());

    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    when(circulationItemService.fetchItemById(anyString())).thenReturn(circulationItem);
    eventListener.handleRequestEvent(CHECK_IN_UNDEFINED_EVENT_SAMPLE, messageHeaders);
    Mockito.verify(transactionRepository, times(0)).save(any());
  }

  @Test
  void handleRequestCancelWithIncorrectDataTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setRole(LENDER);
    MessageHeaders messageHeaders = getMessageHeaders();
    assertDoesNotThrow(() -> eventListener.handleRequestEvent(null, messageHeaders));
  }

  private MessageHeaders getMessageHeaders() {
    Map<String, Object> header = new HashMap<>();
    header.put(XOkapiHeaders.TENANT, TENANT.getBytes());
    return new MessageHeaders(header);
  }
}
