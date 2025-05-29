package org.folio.dcb.service.impl;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;

import java.util.UUID;

import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.DcbPickup;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.Item;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.CirculationRequestService;
import org.folio.dcb.service.EcsRequestTransactionsService;
import org.folio.dcb.service.RequestService;
import org.folio.dcb.utils.RequestStatus;
import org.folio.spring.exception.NotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class EcsRequestTransactionsServiceImpl implements EcsRequestTransactionsService {

  private final BaseLibraryService baseLibraryService;
  private final TransactionRepository transactionRepository;
  private final RequestService requestService;
  private final CirculationRequestService circulationRequestService;
  private final CirculationItemService circulationItemService;

  @Override
  public TransactionStatusResponse createEcsRequestTransactions(String ecsRequestTransactionsId,
    DcbTransaction dcbTransaction) {

    log.info("createEcsRequestTransactions:: creating new transaction request for role {} ",
      dcbTransaction.getRole());
    checkEcsRequestTransactionExistsAndThrow(ecsRequestTransactionsId);
    CirculationRequest circulationRequest = circulationRequestService.fetchRequestById(
      dcbTransaction.getRequestId());
    if (circulationRequest != null && RequestStatus.isRequestOpen(
      RequestStatus.from(circulationRequest.getStatus()))) {
      if (dcbTransaction.getRole() == LENDER) {
        createLenderEcsRequestTransactions(ecsRequestTransactionsId, dcbTransaction, circulationRequest);
      } else if(dcbTransaction.getRole() == BORROWER
        || dcbTransaction.getRole() == PICKUP
        || dcbTransaction.getRole() == BORROWING_PICKUP) {

        createBorrowerEcsRequestTransactions(ecsRequestTransactionsId, dcbTransaction,
          circulationRequest);
      } else {
        throw new IllegalArgumentException("Unimplemented role: " + dcbTransaction.getRole());
      }
      return TransactionStatusResponse.builder()
        .status(TransactionStatusResponse.StatusEnum.CREATED)
        .item(dcbTransaction.getItem())
        .patron(dcbTransaction.getPatron())
        .build();
    } else {
      throw new IllegalArgumentException("Unable to create ECS transaction as could not find open request");
    }
  }

  @Override
  public TransactionStatusResponse updateEcsRequestTransaction(String ecsRequestTransactionsId, DcbTransaction dcbTransaction) {
    log.info("updateEcsRequestTransactions:: updating transaction {}", ecsRequestTransactionsId);
    var transactionResult = transactionRepository.findById(ecsRequestTransactionsId);
    if (transactionResult.isEmpty()) {
      throw new NotFoundException("Transaction with id " + ecsRequestTransactionsId + " not found");
    }
    var item = dcbTransaction.getItem();
    var barcode = item == null ? null : dcbTransaction.getItem().getBarcode();
    var transaction = transactionResult.get();
    transaction.setItemBarcode(barcode);
    transactionRepository.save(transaction);
    log.info("updateEcsRequestTransactions:: updated transaction {} with barcode {}",
      ecsRequestTransactionsId, barcode);

    return TransactionStatusResponse.builder()
      .status(TransactionStatusResponse.StatusEnum.fromValue(transaction.getStatus().getValue()))
      .item(dcbTransaction.getItem())
      .patron(dcbTransaction.getPatron())
      .build();
  }

  private void checkEcsRequestTransactionExistsAndThrow(String dcbTransactionId) {
    if (transactionRepository.existsById(dcbTransactionId)) {
      throw new ResourceAlreadyExistException(
        String.format("unable to create ECS transaction with ID %s as it already exists",
          dcbTransactionId));
    }
  }

  private void createLenderEcsRequestTransactions(String ecsRequestTransactionsId,
    DcbTransaction dcbTransaction, CirculationRequest circulationRequest) {

    dcbTransaction.setItem(DcbItem.builder()
      .id(String.valueOf(circulationRequest.getItemId()))
      .barcode(buildNonEmptyBarcode(circulationRequest.getItem().getBarcode(), circulationRequest.getItemId().toString()))
      .build());
    dcbTransaction.setPatron(DcbPatron.builder()
      .id(String.valueOf(circulationRequest.getRequesterId()))
      .barcode(circulationRequest.getRequester().getBarcode())
      .build());
    dcbTransaction.setPickup(DcbPickup.builder()
      .servicePointId(String.valueOf(circulationRequest.getPickupServicePointId()))
      .build());
    baseLibraryService.saveDcbTransaction(ecsRequestTransactionsId, dcbTransaction,
      dcbTransaction.getRequestId());
  }

  private void createBorrowerEcsRequestTransactions(String ecsRequestTransactionsId,
    DcbTransaction dcbTransaction, CirculationRequest circulationRequest) {

    var itemVirtual = dcbTransaction.getItem();
    if (itemVirtual == null) {
      throw new IllegalArgumentException("Item is required for borrower transaction");
    }
    baseLibraryService.checkItemExistsInInventoryAndThrow(itemVirtual.getBarcode());
    CirculationItem item = circulationItemService.checkIfItemExistsAndCreate(itemVirtual, circulationRequest.getPickupServicePointId());
    circulationRequest.setItemId(UUID.fromString(item.getId()));
    circulationRequest.setItem(Item.builder()
      .barcode(item.getBarcode())
      .build());
    circulationRequest.setHoldingsRecordId(UUID.fromString(item.getHoldingsRecordId()));
    requestService.updateCirculationRequest(circulationRequest);
    dcbTransaction.setPatron(DcbPatron.builder()
      .id(String.valueOf(circulationRequest.getRequesterId()))
      .barcode(buildNonEmptyBarcode(circulationRequest.getRequester().getBarcode(), circulationRequest.getItemId().toString()))
      .build());
    dcbTransaction.setPickup(DcbPickup.builder()
      .servicePointId(String.valueOf(circulationRequest.getPickupServicePointId()))
      .build());
    baseLibraryService.saveDcbTransaction(ecsRequestTransactionsId, dcbTransaction,
      dcbTransaction.getRequestId());
  }

  private String buildNonEmptyBarcode(String barcode, String itemId) {
    if (barcode == null || barcode.isBlank()) {
      log.info("buildNonEmptyBarcode:: barcode is null or empty, using autogenerated barcode: {}",
        itemId);
      return itemId;
    }

    return barcode;
  }
}
