package org.folio.dcb.service.impl;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWING_PICKUP;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.PICKUP;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.DcbPickup;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.Item;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.integration.circulation.model.RequestStatus;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.CirculationRequestService;
import org.folio.dcb.service.EcsRequestTransactionsService;
import org.folio.dcb.service.RequestService;
import org.folio.spring.exception.NotFoundException;
import org.springframework.stereotype.Service;

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
  public TransactionStatusResponse createEcsRequestTransactions(String transactionId, DcbTransaction dcbTransaction) {
    log.info("createEcsRequestTransactions:: creating new transaction request for role {} ", dcbTransaction.getRole());
    checkEcsRequestTransactionExistsAndThrow(transactionId);
    CirculationRequest circulationRequest = circulationRequestService.fetchRequestById(dcbTransaction.getRequestId());

    if (circulationRequest != null && RequestStatus.isRequestOpen(RequestStatus.from(circulationRequest.getStatus()))) {
      if (dcbTransaction.getRole() == LENDER) {
        createLenderEcsRequestTransactions(transactionId, dcbTransaction, circulationRequest);
      } else if (dcbTransaction.getRole() == BORROWER
        || dcbTransaction.getRole() == PICKUP
        || dcbTransaction.getRole() == BORROWING_PICKUP) {

        createBorrowerEcsRequestTransactions(transactionId, dcbTransaction, circulationRequest);
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
  public TransactionStatusResponse updateEcsRequestTransaction(String transactionId, DcbTransaction dcbTransaction) {
    log.info("updateEcsRequestTransactions:: updating transaction {}", transactionId);
    var transactionResult = transactionRepository.findById(transactionId);
    if (transactionResult.isEmpty()) {
      throw new NotFoundException("Transaction with id " + transactionId + " not found");
    }
    var item = dcbTransaction.getItem();
    var barcode = item == null ? null : dcbTransaction.getItem().getBarcode();
    var transaction = transactionResult.get();
    transaction.setItemBarcode(barcode);
    transactionRepository.save(transaction);
    log.info("updateEcsRequestTransactions:: updated transaction {}", transactionId);

    return TransactionStatusResponse.builder()
      .status(TransactionStatusResponse.StatusEnum.fromValue(transaction.getStatus().getValue()))
      .item(dcbTransaction.getItem())
      .patron(dcbTransaction.getPatron())
      .build();
  }

  private void checkEcsRequestTransactionExistsAndThrow(String dcbTransactionId) {
    if (transactionRepository.existsById(dcbTransactionId)) {
      throw new ResourceAlreadyExistException(String.format(
        "unable to create ECS transaction with ID %s as it already exists", dcbTransactionId));
    }
  }

  private void createLenderEcsRequestTransactions(String ecsRequestTransactionsId, DcbTransaction dcbTransaction,
    CirculationRequest circRequest) {

    dcbTransaction.setItem(DcbItem.builder()
      .id(String.valueOf(circRequest.getItemId()))
      .barcode(buildNonEmptyBarcode(circRequest.getItem().getBarcode(), circRequest.getItemId().toString()))
      .build());
    dcbTransaction.setPatron(DcbPatron.builder()
      .id(String.valueOf(circRequest.getRequesterId()))
      .barcode(circRequest.getRequester().getBarcode())
      .build());
    dcbTransaction.setPickup(DcbPickup.builder()
      .servicePointId(String.valueOf(circRequest.getPickupServicePointId()))
      .build());
    baseLibraryService.saveDcbTransaction(ecsRequestTransactionsId, dcbTransaction,
      dcbTransaction.getRequestId());
  }

  private void createBorrowerEcsRequestTransactions(String ecsRequestTransactionsId, DcbTransaction dcbTransaction,
    CirculationRequest circRequest) {

    var itemVirtual = dcbTransaction.getItem();
    if (itemVirtual == null) {
      throw new IllegalArgumentException("Item is required for borrower transaction");
    }
    baseLibraryService.checkItemExistsInInventoryAndThrow(itemVirtual.getBarcode());
    var item = circulationItemService.checkIfItemExistsAndCreate(itemVirtual,
      circRequest.getPickupServicePointId(), true);
    circRequest.setItemId(UUID.fromString(item.getId()));
    circRequest.setItem(Item.builder()
      .barcode(item.getBarcode())
      .build());
    circRequest.setHoldingsRecordId(UUID.fromString(item.getHoldingsRecordId()));
    requestService.updateCirculationRequest(circRequest);
    dcbTransaction.setPatron(DcbPatron.builder()
      .id(String.valueOf(circRequest.getRequesterId()))
      .barcode(buildNonEmptyBarcode(circRequest.getRequester().getBarcode(), circRequest.getItemId().toString()))
      .build());
    dcbTransaction.setPickup(DcbPickup.builder()
      .servicePointId(String.valueOf(circRequest.getPickupServicePointId()))
      .build());
    baseLibraryService.saveDcbTransaction(ecsRequestTransactionsId, dcbTransaction,
      dcbTransaction.getRequestId());
  }

  private static String buildNonEmptyBarcode(String barcode, String itemId) {
    if (barcode == null || barcode.isBlank()) {
      log.info("buildNonEmptyBarcode:: barcode is null or empty, using autogenerated value.");
      return itemId;
    }

    return barcode;
  }
}
