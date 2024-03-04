package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.DcbPickup;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.CirculationRequestService;
import org.folio.dcb.service.EcsTlrTransactionsService;
import org.folio.dcb.utils.RequestStatus;
import org.springframework.stereotype.Service;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;

@Service
@RequiredArgsConstructor
@Log4j2
public class EcsTlrTransactionsServiceImpl implements EcsTlrTransactionsService {

  private final BaseLibraryService baseLibraryService;
  private final TransactionRepository transactionRepository;
  private final CirculationRequestService circulationRequestService;

  @Override
  public TransactionStatusResponse createEcsTlrTransaction(String dcbTransactionId, DcbTransaction dcbTransaction) {
    log.info("createCirculationRequest:: creating new transaction request for role {} ",
      dcbTransaction.getRole());
    checkEcsTransactionExistsAndThrow(dcbTransactionId);
    CirculationRequest circulationRequest = circulationRequestService.fetchRequestById(dcbTransaction.getRequestId());
    if(circulationRequest != null && RequestStatus.isRequestOpen(RequestStatus.from(circulationRequest.getStatus()))) {
      if (dcbTransaction.getRole() == LENDER) {
        dcbTransaction.setItem(DcbItem.builder()
          .id(String.valueOf(circulationRequest.getItemId()))
            .barcode(circulationRequest.getItem().getBarcode())
          .build());
        dcbTransaction.setPatron(DcbPatron.builder()
          .id(String.valueOf(circulationRequest.getRequesterId()))
          .barcode(circulationRequest.getRequester().getBarcode())
          .build());
        dcbTransaction.setPickup(DcbPickup.builder()
          .servicePointId(String.valueOf(circulationRequest.getPickupServicePointId()))
          .build());
        baseLibraryService.saveDcbTransaction(dcbTransactionId, dcbTransaction, dcbTransaction.getRequestId());
      } else {
        throw new IllegalArgumentException("Unimplemented role: " + dcbTransaction.getRole());
      }
      return TransactionStatusResponse.builder()
        .status(TransactionStatusResponse.StatusEnum.CREATED)
        .item(dcbTransaction.getItem())
        .patron(dcbTransaction.getPatron())
        .build();
    } else {
      throw new IllegalArgumentException("Unable to create ecs transaction as could not find open request");
    }
  }

  private void checkEcsTransactionExistsAndThrow(String dcbTransactionId) {
    if (transactionRepository.existsById(dcbTransactionId)) {
      throw new ResourceAlreadyExistException(
        String.format("unable to create ECS transaction with ID %s as it already exists",
          dcbTransactionId));
    }
  }
}
