package org.folio.dcb.listener;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;
import static org.folio.dcb.utils.EntityUtils.createCirculationItem;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.getMockDataAsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.dcb.domain.dto.ItemStatus;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.folio.dcb.listener.kafka.CirculationEventListener;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.CirculationItemService;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class CirculationRequestEventListenerTest extends BaseTenantIntegrationTest {

  private static final String TENANT = "diku";
  private static final String REQUEST_EVENT_SAMPLE_NON_DCB = getMockDataAsString("mockdata/kafka/request_sample.json");

  private static final String CHECK_IN_EVENT_SAMPLE_FOR_DCB = getMockDataAsString("mockdata/kafka/check_in_dcb.json");
  private static final String CHECK_IN_DELIVERY_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/check_in_awaiting_delivery.json");
  private static final String CHECK_IN_TRANSIT_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/check_in_transit.json");
  private static final String CHECK_IN_TRANSIT_EVENT_FOR_DCB_SAMPLE = getMockDataAsString("mockdata/kafka/check_in_transit_dcb.json");
  private static final String CHECK_IN_UNDEFINED_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/request_undefined.json");
  private static final String REQUEST_CANCEL_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/cancel_request.json");
  private static final String REQUEST_CANCEL_EVENT_FOR_DCB_SAMPLE = getMockDataAsString("mockdata/kafka/cancel_request_dcb.json");
  private static final String REQUEST_EXPIRED_EVENT_FOR_DCB_SAMPLE = getMockDataAsString("mockdata/kafka/expired_request_dcb.json");
  private static final String CANCELLATION_DCB_REREQUEST_TRUE_SAMPLE = getMockDataAsString(
    "mockdata/kafka/cancellation_dcb_rerequest_true.json");
  private static final String CANCELLATION_DCB_REREQUEST_FALSE_SAMPLE = getMockDataAsString(
    "mockdata/kafka/cancellation_dcb_rerequest_false.json");
  private static final String CANCELLATION_DCB_REREQUEST_WITHOUT_SAMPLE = getMockDataAsString(
    "mockdata/kafka/cancellation_dcb_rerequest_without_dcb_rerequest_property.json");

  @Autowired
  private CirculationEventListener eventListener ;

  @MockitoBean
  private TransactionRepository transactionRepository;

  @MockitoBean
  private CirculationItemService circulationItemService;

  @Test
  void handleNonDcbRequestTest() {
    MessageHeaders messageHeaders = getMessageHeaders();
    eventListener.handleRequestEvent(REQUEST_EVENT_SAMPLE_NON_DCB, messageHeaders);
    Mockito.verify(transactionRepository, times(0)).save(any());
  }

  @ParameterizedTest
  @MethodSource("pathToExecutionTimes")
  void handleCancelRequestEventWhenTransactionDcbUpdates(String path, int executionTimes) {
    var transactionEntity = createTransactionEntity();
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(any()))
      .thenReturn(Optional.of(transactionEntity));
    eventListener.handleRequestEvent(path, messageHeaders);
    Mockito.verify(transactionRepository, times(executionTimes)).save(any());
  }

  private static Stream<Arguments> pathToExecutionTimes() {
    return Stream.of(
      Arguments.of(CANCELLATION_DCB_REREQUEST_FALSE_SAMPLE, 1),
      Arguments.of(CANCELLATION_DCB_REREQUEST_TRUE_SAMPLE, 0),
      Arguments.of(CANCELLATION_DCB_REREQUEST_WITHOUT_SAMPLE, 1)
    );
  }

  @Test
  void handleCheckInEventInPickupForDcbFromOpenToAwaitingPickupTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("f660129e-4520-4dec-a73e-4c0140bb1ba3");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.OPEN);
    transactionEntity.setRole(PICKUP);
    var circulationItem = createCirculationItem();
    circulationItem.setStatus(org.folio.dcb.domain.dto.ItemStatus.builder().name(org.folio.dcb.domain.dto.ItemStatus.NameEnum.AWAITING_PICKUP).build());
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    when(circulationItemService.fetchItemById(anyString())).thenReturn(circulationItem);
    eventListener.handleRequestEvent(CHECK_IN_EVENT_SAMPLE_FOR_DCB, messageHeaders);
    Mockito.verify(transactionRepository).save(any());
  }

  @Test
  void handleCheckInEventDeliveryForDcbFromOpenToAwaitingPickupTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId(UUID.randomUUID().toString());
    transactionEntity.setStatus(TransactionStatus.StatusEnum.OPEN);
    transactionEntity.setRole(PICKUP);
    var circulationItem = createCirculationItem();
    circulationItem.setStatus(org.folio.dcb.domain.dto.ItemStatus.builder()
      .name(org.folio.dcb.domain.dto.ItemStatus.NameEnum.AWAITING_PICKUP).build());
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(any()))
      .thenReturn(Optional.of(transactionEntity));
    when(circulationItemService.fetchItemById(anyString())).thenReturn(circulationItem);
    eventListener.handleRequestEvent(CHECK_IN_DELIVERY_EVENT_SAMPLE, messageHeaders);
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
    when(transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    when(circulationItemService.fetchItemById(anyString())).thenReturn(circulationItem);
    MessageHeaders messageHeaders = getMessageHeaders();
    eventListener.handleRequestEvent(CHECK_IN_EVENT_SAMPLE_FOR_DCB, messageHeaders);
    Mockito.verify(transactionRepository).save(any());
  }

  @Test
  void handleCancelRequestTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setRole(LENDER);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleRequestEvent(REQUEST_CANCEL_EVENT_SAMPLE, messageHeaders);
    Mockito.verify(transactionRepository, times(1)).save(any());
  }

  @Test
  void handleCancelRequestForDcbTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setRole(LENDER);
    MessageHeaders messageHeaders = getMessageHeaders();
    when(transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    eventListener.handleRequestEvent(REQUEST_CANCEL_EVENT_FOR_DCB_SAMPLE, messageHeaders);
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
    when(transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    when(circulationItemService.fetchItemById(anyString())).thenReturn(circulationItem);
    MessageHeaders messageHeaders = getMessageHeaders();
    eventListener.handleRequestEvent(CHECK_IN_TRANSIT_EVENT_SAMPLE, messageHeaders);
    Mockito.verify(transactionRepository, times(1)).save(any());
  }

  @Test
  void handleOpenRequestForDcbTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setItemId("5b95877e-86c0-4cb7-a0cd-7660b348ae5d");
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionEntity.setRole(LENDER);

    var circulationItem = createCirculationItem();
    circulationItem.setStatus(org.folio.dcb.domain.dto.ItemStatus.builder().name(ItemStatus.NameEnum.IN_TRANSIT).build());
    when(transactionRepository.findTransactionByRequestIdAndStatusNotInClosed(any())).thenReturn(Optional.of(transactionEntity));
    when(circulationItemService.fetchItemById(anyString())).thenReturn(circulationItem);
    MessageHeaders messageHeaders = getMessageHeaders();
    eventListener.handleRequestEvent(CHECK_IN_TRANSIT_EVENT_FOR_DCB_SAMPLE, messageHeaders);
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
