package org.folio.dcb.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.RequestService;
import org.folio.dcb.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;

@Service("lendingLibraryService")
@RequiredArgsConstructor
@Log4j2
public class LendingLibraryServiceImpl implements LibraryService {

  private final UserService userService;
  private final RequestService requestService;
  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public TransactionStatusResponse createTransaction(String dcbTransactionId, DcbTransaction dcbTransaction) {
    log.debug("createTransaction:: creating a new transaction with dcbTransactionId {} , dcbTransaction {}",
      dcbTransactionId, dcbTransaction);

    checkTransactionExistsAndThrow(dcbTransactionId);
    var item = dcbTransaction.getItem();
    var patron = dcbTransaction.getPatron();

    var user = userService.fetchOrCreateUser(patron);
    requestService.createPageItemRequest(user, item);
    saveDcbTransaction(dcbTransactionId, dcbTransaction);

    return TransactionStatusResponse.builder()
      .status(TransactionStatusResponse.StatusEnum.CREATED)
      .item(item)
      .patron(patron)
      .build();
  }

  private void checkTransactionExistsAndThrow(String dcbTransactionId) {
    if(transactionRepository.existsById(dcbTransactionId)) {
      throw new ResourceAlreadyExistException(
        String.format("unable to create transaction with id %s as it already exists", dcbTransactionId));
    }
  }

  private void saveDcbTransaction(String dcbTransactionId, DcbTransaction dcbTransaction) {
    TransactionEntity transactionEntity = transactionMapper.mapToEntity(dcbTransactionId, dcbTransaction);
    if (Objects.isNull(transactionEntity)) {
      throw new IllegalArgumentException("Transaction Entity is null");
    }
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    transactionRepository.save(transactionEntity);
  }

  @Override
  public void updateTransactionStatus(String checkInEvent) {
    var checkInItemId = parseCheckInEvent(checkInEvent);
    log.info("updateTransactionStatus:: Received checkIn event for itemId: {}", checkInItemId);

    if (Objects.nonNull(checkInItemId)) {
      transactionRepository.findTransactionByItemId(checkInItemId)
        .ifPresent(transactionEntity -> {
          if (LENDER.equals(transactionEntity.getRole())
            && TransactionStatus.StatusEnum.CREATED.equals(transactionEntity.getStatus())) {
            transactionEntity.setStatus(TransactionStatus.StatusEnum.OPEN);
            transactionRepository.save(transactionEntity);
          }
        });
    }
  }

  private String parseCheckInEvent(String eventPayload) {
    try {
      JsonNode jsonNode = objectMapper.readTree(eventPayload);
      JsonNode dataNode = jsonNode.get("data");
      JsonNode newDataNode = (dataNode != null) ? dataNode.get("new") : null;

      if (newDataNode != null && newDataNode.has("itemId")) {
        return newDataNode.get("itemId").asText();
      }
    } catch (Exception e) {
      log.error("Could not parse input payload for processing checkIn event", e);
    }

    return null;
  }
}
