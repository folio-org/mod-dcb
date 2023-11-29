package org.folio.dcb.service;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.StatusProcessor;
import org.folio.dcb.domain.dto.TransactionStatus;

import java.util.ArrayList;
import java.util.List;

import org.folio.dcb.exception.StatusException;
import org.springframework.stereotype.Component;

import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.*;

@Data
@Component
@Log4j2
public class StatusProcessorService {
  private StatusProcessor chain;

  private static String STATUS_TRANSITION_ERROR_MSG = "Status transition will not be possible from %s to %s";

  public List<TransactionStatus.StatusEnum> lendingChainProcessor(TransactionStatus.StatusEnum fromStatus, TransactionStatus.StatusEnum toStatus) {
    log.debug("lendingChainProcessor:: fetching list of statuses from {} to {}", fromStatus, toStatus);
    StatusProcessorService startChain = new StatusProcessorService();
    StatusProcessor closeProcessor = new StatusProcessor(ITEM_CHECKED_IN, CLOSED, true, null);
    StatusProcessor checkInProcessor = new StatusProcessor(ITEM_CHECKED_OUT, ITEM_CHECKED_IN, false, closeProcessor);
    StatusProcessor checkoutProcessor = new StatusProcessor(AWAITING_PICKUP, ITEM_CHECKED_OUT, false, checkInProcessor);
    StatusProcessor awaitingPickupProcessor = new StatusProcessor(OPEN, AWAITING_PICKUP, false, checkoutProcessor);
    StatusProcessor openProcessor = new StatusProcessor(CREATED, OPEN, true, awaitingPickupProcessor);
    startChain.setChain(openProcessor);
    var statuses = process(startChain, fromStatus, toStatus);
    log.info("lendingChainProcessor:: Following statuses needs to be transitioned {} ", statuses);
    return statuses;
  }

  public List<TransactionStatus.StatusEnum> borrowingChainProcessor(TransactionStatus.StatusEnum fromStatus, TransactionStatus.StatusEnum toStatus) {
    log.debug("borrowingChainProcessor:: fetching list of statuses from {} to {}", fromStatus, toStatus);
    StatusProcessorService startChain = new StatusProcessorService();
    StatusProcessor closeProcessor = new StatusProcessor(ITEM_CHECKED_IN, CLOSED, false, null);
    StatusProcessor checkInProcessor = new StatusProcessor(ITEM_CHECKED_OUT, ITEM_CHECKED_IN, false, closeProcessor);
    StatusProcessor checkoutProcessor = new StatusProcessor(AWAITING_PICKUP, ITEM_CHECKED_OUT, false, checkInProcessor);
    StatusProcessor awaitingPickupProcessor = new StatusProcessor(OPEN, AWAITING_PICKUP, false, checkoutProcessor);
    StatusProcessor openProcessor = new StatusProcessor(CREATED, OPEN, false, awaitingPickupProcessor);
    startChain.setChain(openProcessor);
    var statuses = process(startChain, fromStatus, toStatus);
    log.info("borrowingChainProcessor:: Following statuses needs to be transitioned {} ", statuses);
    return statuses;
  }

  private List<TransactionStatus.StatusEnum> process(StatusProcessorService statusProcessorService, TransactionStatus.StatusEnum fromStatus, TransactionStatus.StatusEnum toStatus) {
    if(fromStatus.ordinal()>toStatus.ordinal()) {
      throw new StatusException(String.format(STATUS_TRANSITION_ERROR_MSG, fromStatus, toStatus));
    }
    List<TransactionStatus.StatusEnum> transactionStatuses = new ArrayList<>();
    if(CANCELLED == toStatus) {
      transactionStatuses.add(CANCELLED);
      return transactionStatuses;
    }
    var processor = statusProcessorService.getChain();
    while (processor != null) {
      if (processor.getCurrentStatus().ordinal() >= fromStatus.ordinal() &&
        processor.getNextStatus().ordinal() <= toStatus.ordinal()) {
        if (processor.isManual()) {
          throw new IllegalArgumentException(String.format(STATUS_TRANSITION_ERROR_MSG, fromStatus, toStatus));
        } else {
          transactionStatuses.add(processor.getNextStatus());
        }
      }
      processor = processor.getNextProcessor();
    }
    return transactionStatuses;
  }

}
