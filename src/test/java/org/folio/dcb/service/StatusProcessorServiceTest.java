package org.folio.dcb.service;

import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.exception.StatusException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)

class StatusProcessorServiceTest {

  @InjectMocks
  StatusProcessorService statusProcessorService;

  @Test
  void lendingChainProcessorTest() {
    var statusEnumList = statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    assertEquals(List.of(TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT, TransactionStatus.StatusEnum.ITEM_CHECKED_IN), statusEnumList);

    statusEnumList = statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    assertEquals(List.of(TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT), statusEnumList);

    statusEnumList = statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.AWAITING_PICKUP);
    assertEquals(List.of(TransactionStatus.StatusEnum.AWAITING_PICKUP), statusEnumList);

    statusEnumList = statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    assertEquals(List.of(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT, TransactionStatus.StatusEnum.ITEM_CHECKED_IN), statusEnumList);

    statusEnumList = statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.CREATED, TransactionStatus.StatusEnum.CANCELLED);
    assertEquals(List.of(TransactionStatus.StatusEnum.CANCELLED), statusEnumList);

    statusEnumList = statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.CANCELLED);
    assertEquals(List.of(TransactionStatus.StatusEnum.CANCELLED), statusEnumList);

    statusEnumList = statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.CANCELLED);
    assertEquals(List.of(TransactionStatus.StatusEnum.CANCELLED), statusEnumList);
  }

  @Test
  void lendingChainProcessorErrorTest() {
    assertThrows(StatusException.class, () -> statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.CREATED, TransactionStatus.StatusEnum.CLOSED));

    assertThrows(StatusException.class, () -> statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.CREATED, TransactionStatus.StatusEnum.CREATED));

    assertThrows(StatusException.class, () -> statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.CLOSED));

    assertThrows(StatusException.class, () -> statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.CLOSED));

    assertThrows(StatusException.class, () -> statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT, TransactionStatus.StatusEnum.CLOSED));

    assertThrows(StatusException.class, () -> statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.ITEM_CHECKED_IN, TransactionStatus.StatusEnum.CLOSED));
  }

  @Test
  void borrowingChainProcessorTest() {
    var statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.CREATED, TransactionStatus.StatusEnum.CLOSED);
    assertEquals(List.of(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT, TransactionStatus.StatusEnum.ITEM_CHECKED_IN, TransactionStatus.StatusEnum.CLOSED), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.CREATED, TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    assertEquals(List.of(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT, TransactionStatus.StatusEnum.ITEM_CHECKED_IN), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.CREATED, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    assertEquals(List.of(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.CREATED, TransactionStatus.StatusEnum.AWAITING_PICKUP);
    assertEquals(List.of(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.AWAITING_PICKUP), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.CREATED, TransactionStatus.StatusEnum.OPEN);
    assertEquals(List.of(TransactionStatus.StatusEnum.OPEN), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.CLOSED);
    assertEquals(List.of(TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT, TransactionStatus.StatusEnum.ITEM_CHECKED_IN, TransactionStatus.StatusEnum.CLOSED), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    assertEquals(List.of(TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT, TransactionStatus.StatusEnum.ITEM_CHECKED_IN), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    assertEquals(List.of(TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.AWAITING_PICKUP);
    assertEquals(List.of(TransactionStatus.StatusEnum.AWAITING_PICKUP), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.CLOSED);
    assertEquals(List.of(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT, TransactionStatus.StatusEnum.ITEM_CHECKED_IN, TransactionStatus.StatusEnum.CLOSED), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    assertEquals(List.of(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT, TransactionStatus.StatusEnum.ITEM_CHECKED_IN), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.ITEM_CHECKED_OUT);
    assertEquals(List.of(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT, TransactionStatus.StatusEnum.CLOSED);
    assertEquals(List.of(TransactionStatus.StatusEnum.ITEM_CHECKED_IN, TransactionStatus.StatusEnum.CLOSED), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.ITEM_CHECKED_OUT, TransactionStatus.StatusEnum.ITEM_CHECKED_IN);
    assertEquals(List.of(TransactionStatus.StatusEnum.ITEM_CHECKED_IN), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.ITEM_CHECKED_IN, TransactionStatus.StatusEnum.CLOSED);
    assertEquals(List.of(TransactionStatus.StatusEnum.CLOSED), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.CREATED, TransactionStatus.StatusEnum.CANCELLED);
    assertEquals(List.of(TransactionStatus.StatusEnum.CANCELLED), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.CANCELLED);
    assertEquals(List.of(TransactionStatus.StatusEnum.CANCELLED), statusEnumList);

    statusEnumList = statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.AWAITING_PICKUP, TransactionStatus.StatusEnum.CANCELLED);
    assertEquals(List.of(TransactionStatus.StatusEnum.CANCELLED), statusEnumList);
  }

  @Test
  void borrowingChainProcessorErrorTest() {
    assertThrows(StatusException.class, () -> statusProcessorService.borrowingChainProcessor(TransactionStatus.StatusEnum.CLOSED, TransactionStatus.StatusEnum.OPEN));

    assertThrows(StatusException.class, () -> statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.CLOSED, TransactionStatus.StatusEnum.CLOSED));

    assertThrows(StatusException.class, () -> statusProcessorService.lendingChainProcessor(TransactionStatus.StatusEnum.OPEN, TransactionStatus.StatusEnum.CREATED));
  }
}
