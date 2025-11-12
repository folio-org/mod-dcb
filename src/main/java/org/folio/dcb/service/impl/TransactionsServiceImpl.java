package org.folio.dcb.service.impl;

import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_OUT;
import static org.folio.dcb.utils.TransactionDetailsUtil.rolesNotEqual;
import static org.folio.dcb.utils.TransactionDetailsUtil.statusesNotEqual;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.client.feign.CirculationLoanPolicyStorageClient;
import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.DcbUpdateTransaction;
import org.folio.dcb.domain.dto.Loan;
import org.folio.dcb.domain.dto.LoanCollection;
import org.folio.dcb.domain.dto.LoanPolicy;
import org.folio.dcb.domain.dto.LoanPolicyCollection;
import org.folio.dcb.domain.dto.RenewByIdRequest;
import org.folio.dcb.domain.dto.RenewByIdResponse;
import org.folio.dcb.domain.dto.RenewalInfo;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.dto.TransactionStatusResponseCollection;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.exception.StatusException;
import org.folio.dcb.repository.TransactionAuditRepository;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.LibraryService;
import org.folio.dcb.service.StatusProcessorService;
import org.folio.dcb.service.TransactionsService;
import org.folio.dcb.utils.CqlQuery;
import org.folio.spring.exception.NotFoundException;
import org.folio.util.PercentCodec;
import org.folio.util.StringUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class TransactionsServiceImpl implements TransactionsService {

  private static final String CQL_AND = " AND ";
  private static final int UNLIMITED = -1;
  @Qualifier("lendingLibraryService")
  private final LibraryService lendingLibraryService;
  @Qualifier("borrowingPickupLibraryService")
  private final LibraryService borrowingPickupLibraryService;
  @Qualifier("pickupLibraryService")
  private final LibraryService pickupLibraryService;
  @Qualifier("borrowingLibraryService")
  private final LibraryService borrowingLibraryService;
  private final TransactionRepository transactionRepository;
  private final StatusProcessorService statusProcessorService;
  private final TransactionMapper transactionMapper;
  private final TransactionAuditRepository transactionAuditRepository;
  private final BaseLibraryService baseLibraryService;
  private final CirculationClient circulationClient;
  private final CirculationLoanPolicyStorageClient circulationLoanPolicyStorageClient;

  @Override
  public TransactionStatusResponse createCirculationRequest(String dcbTransactionId, DcbTransaction dcbTransaction) {
    log.debug("createCirculationRequest:: creating new transaction request for role {} ", dcbTransaction.getRole());
    checkTransactionExistsAndThrow(dcbTransactionId);

    return switch (dcbTransaction.getRole()) {
      case LENDER -> lendingLibraryService.createCirculation(dcbTransactionId, dcbTransaction);
      case BORROWING_PICKUP -> borrowingPickupLibraryService.createCirculation(dcbTransactionId, dcbTransaction);
      case PICKUP -> pickupLibraryService.createCirculation(dcbTransactionId, dcbTransaction);
      case BORROWER -> borrowingLibraryService.createCirculation(dcbTransactionId, dcbTransaction);
    };
  }

  @Override
  public TransactionStatusResponse updateTransactionStatus(String dcbTransactionId, TransactionStatus transactionStatus) {
    return transactionRepository.findById(dcbTransactionId).map(dcbTransaction -> {
      if (dcbTransaction.getStatus() == transactionStatus.getStatus()) {
        throw new StatusException(String.format(
          "Current transaction status equal to new transaction status: dcbTransactionId: %s, status: %s", dcbTransactionId, transactionStatus.getStatus()
        ));
      } else if (transactionStatus.getStatus() == TransactionStatus.StatusEnum.CANCELLED
        && (dcbTransaction.getStatus() == TransactionStatus.StatusEnum.ITEM_CHECKED_IN ||
        dcbTransaction.getStatus() == TransactionStatus.StatusEnum.ITEM_CHECKED_OUT) ||
        dcbTransaction.getStatus() == TransactionStatus.StatusEnum.CLOSED) {
        throw new StatusException(String.format(
          "Cannot cancel transaction dcbTransactionId: %s. Transaction already in status: %s: ", dcbTransactionId, dcbTransaction.getStatus()
        ));
      }
      switch (dcbTransaction.getRole()) {
        case LENDER -> statusProcessorService.lendingChainProcessor(dcbTransaction.getStatus(), transactionStatus.getStatus())
          .forEach(statusEnum -> lendingLibraryService.updateTransactionStatus(dcbTransaction, TransactionStatus.builder().status(statusEnum).build()));
        case BORROWING_PICKUP -> borrowingPickupLibraryService.updateTransactionStatus(dcbTransaction, transactionStatus);
        case PICKUP -> pickupLibraryService.updateTransactionStatus(dcbTransaction, transactionStatus);
        case BORROWER -> statusProcessorService.borrowingChainProcessor(dcbTransaction.getStatus(), transactionStatus.getStatus())
          .forEach(statusEnum -> borrowingLibraryService.updateTransactionStatus(dcbTransaction, TransactionStatus.builder().status(statusEnum).build()));
      }

      return TransactionStatusResponse.builder()
        .status(TransactionStatusResponse.StatusEnum.fromValue(transactionStatus.getStatus().getValue()))
        .build();
    }).orElseThrow(() -> new IllegalArgumentException(String.format("Transaction with id %s not found", dcbTransactionId)));
  }

  @Override
  public TransactionStatusResponse getTransactionStatusById(String dcbTransactionId) {
    log.debug("getTransactionStatusById:: id {} ", dcbTransactionId);
    var transactionEntity = getTransactionEntityOrThrow(dcbTransactionId);
    var loanRenewalDetails = getLoanRenewalDetails(transactionEntity);
    return generateTransactionStatusResponseFromTransactionEntity(transactionEntity, loanRenewalDetails);
  }

  private Optional<LoanRenewalDetails> getLoanRenewalDetails(TransactionEntity transactionEntity) {
    if (isTxnItemCheckoutAndRoleIsBorrowerOrBorrowingPickup(transactionEntity)) {
      String loanQuery = buildLoanQuery(transactionEntity);
      LoanCollection loanCollection = circulationClient.fetchLoanByQuery(loanQuery);
      if (loanCollection.getLoans().isEmpty()) {
        return Optional.empty();
      }
      Loan firstLoan = loanCollection.getLoans().get(0);
      Integer loanRenewalCount =
              Integer.valueOf(Optional.ofNullable(firstLoan.getRenewalCount()).orElse("0"));

      String loanPolicyIdQuery = "id==" + StringUtil.cqlEncode(firstLoan.getLoanPolicyId());
      LoanPolicyCollection loanPolicyCollection =
              circulationLoanPolicyStorageClient.fetchLoanPolicyByQuery(PercentCodec.encode(loanPolicyIdQuery).toString());
      LoanPolicy firstLoanPolicy = loanPolicyCollection.getLoanPolicies().get(0);
      Boolean renewable = firstLoanPolicy.getRenewable();

      if (Boolean.FALSE.equals(firstLoanPolicy.getRenewable())) {
        return Optional.of(new LoanRenewalDetails(loanRenewalCount, null, renewable));
      }

      Boolean isUnlimited = firstLoanPolicy.getRenewalsPolicy().getUnlimited();
      Integer renewalMaxCount = Boolean.TRUE.equals(isUnlimited)
              ? UNLIMITED
              : firstLoanPolicy.getRenewalsPolicy().getNumberAllowed();

      return Optional.of(new LoanRenewalDetails(loanRenewalCount, renewalMaxCount, renewable));
    } else {
      return Optional.empty();
    }
  }

  private static boolean isTxnItemCheckoutAndRoleIsBorrowerOrBorrowingPickup(TransactionEntity transactionEntity) {
    return transactionEntity.getStatus() == ITEM_CHECKED_OUT
            && (transactionEntity.getRole() == DcbTransaction.RoleEnum.BORROWING_PICKUP
            || transactionEntity.getRole() == DcbTransaction.RoleEnum.BORROWER);
  }

  private static String buildLoanQuery(TransactionEntity transactionEntity) {
    String itemId = "itemId==" + StringUtil.cqlEncode(transactionEntity.getItemId());
    String statusOpen = "status.name==" + StringUtil.cqlEncode("OPEN");
    String isDCB = "isDcb==" + StringUtil.cqlEncode("true");
    String userId = "userId==" + StringUtil.cqlEncode(transactionEntity.getPatronId());
    return PercentCodec.encode(itemId + CQL_AND + statusOpen + CQL_AND + isDCB + CQL_AND + userId).toString();
  }

  private record LoanRenewalDetails(Integer loanRenewalCount, Integer renewalMaxCount, Boolean renewable) {}

  @Override
  public TransactionStatusResponseCollection getTransactionStatusList(OffsetDateTime fromDate, OffsetDateTime toDate, Integer pageNumber, Integer pageSize) {
    log.info("getTransactionStatusList:: fromDate {}, toDate {}, pageNumber {}, pageSize {}",
      fromDate, toDate, pageNumber, pageSize);
    var pageable = PageRequest.of(pageNumber, pageSize, Sort.by("created_Date"));
    var transactionAuditEntityPage= transactionAuditRepository.findUpdatedTransactionsByDateRange(fromDate, toDate, pageable);
    var transactionStatusResponseList= transactionMapper.mapToDto(transactionAuditEntityPage);
    var totalRecords = (int)transactionAuditEntityPage.getTotalElements();
    var maxPageNumber = pageSize >= totalRecords ? 0 : (int) Math.ceil((double) totalRecords / pageSize) - 1;
    return TransactionStatusResponseCollection
      .builder()
      .transactions(transactionStatusResponseList)
      .totalRecords(totalRecords)
      .currentPageNumber(pageNumber)
      .currentPageSize(pageSize)
      .maximumPageNumber(maxPageNumber)
      .build();
  }

  @Override
  public TransactionStatusResponse renewLoanByTransactionId(String dcbTransactionId) {
    log.info("renewLoanByTransactionId:: getting transaction by id {} ", dcbTransactionId);
    var transaction = getTransactionEntityOrThrow(dcbTransactionId);
    validateTransactionForRenewal(transaction);
    var itemId = transaction.getItemId();
    var patronId = transaction.getPatronId();
    var renewalResponse = circulationClient.renewById(buildRenewRequest(itemId, patronId));
    log.debug("renewLoanByTransactionId:: Renew response {}", renewalResponse);
    validateRenewalResponse(dcbTransactionId, renewalResponse, itemId);
    var loanPolicy = circulationLoanPolicyStorageClient.fetchLoanPolicyById(
      renewalResponse.getLoanPolicyId());
    log.info("renewLoanByTransactionId:: Loan policy response {}", loanPolicy);
    validateLoanPolicy(renewalResponse.getLoanPolicyId(), loanPolicy);
    var loanRenewalDetails = Optional.of(new LoanRenewalDetails(renewalResponse.getRenewalCount(),
      loanPolicy.getRenewalsPolicy().getNumberAllowed(), loanPolicy.getRenewable()));
    return generateTransactionStatusResponseFromTransactionEntity(transaction, loanRenewalDetails);
  }

  @Override
  public void blockItemRenewalByTransactionId(String dcbTransactionId) {
    var transactionEntity = getTransactionEntityOrThrow(dcbTransactionId);
    if (!isTxnItemCheckoutAndRoleIsBorrowerOrBorrowingPickup(transactionEntity)) {
      throw new StatusException("DCB transaction has invalid state for renewal block. "
        + "Item must be already checked out by borrower or borrowing pickup role.");
    }

    setRenewalNumberForVirtualLoan(transactionEntity, Integer.MAX_VALUE);
  }

  @Override
  public void unblockItemRenewalByTransactionId(String dcbTransactionId) {
    var transactionEntity = getTransactionEntityOrThrow(dcbTransactionId);
    if (!isTxnItemCheckoutAndRoleIsBorrowerOrBorrowingPickup(transactionEntity)) {
      throw new StatusException("DCB transaction has invalid state for renewal unblock. "
        + "Item must be already checked out by borrower or borrowing pickup role.");
    }

    setRenewalNumberForVirtualLoan(transactionEntity, 0);
  }

  private void validateLoanPolicy(String loanPolicyId, LoanPolicy loanPolicy) {
    if (Objects.isNull(loanPolicy)) {
      log.debug("validateLoanPolicy:: Loan policy is null");
      throw new NotFoundException(String.format("Loan policy not found for loan id: %s", loanPolicyId));
    }
  }

  private static void validateRenewalResponse(String dcbTransactionId, RenewByIdResponse response,
    String itemId) {
    if(Objects.isNull(response)) {
      log.debug("validateRenewalResponse:: renewal response is null");
      throw new NotFoundException(String.format("Renew failed. Transaction id:%s, Item id: %s",
        dcbTransactionId, itemId));
    }
  }

  private static RenewByIdRequest buildRenewRequest(String itemId, String patronId) {
    return RenewByIdRequest.builder()
      .itemId(itemId)
      .userId(patronId)
      .build();
  }

  private static void validateTransactionForRenewal(TransactionEntity transaction) {
    TransactionStatus.StatusEnum status = transaction.getStatus();
    DcbTransaction.RoleEnum role = transaction.getRole();
    if (statusesNotEqual(ITEM_CHECKED_OUT, status)) {
      log.debug("validateTransactionForRenewal:: Transaction status is {}", status);
      throw new StatusException(String.format(
        "Loan couldn't be renewed with transaction status %s, it could be renewed only with " +
          "ITEM_CHECKED_OUT status", status));
    }
    if (rolesNotEqual(LENDER, role)) {
      log.debug("validateTransactionForRenewal:: Transaction role is {}", role);
      throw new IllegalArgumentException(
        String.format("Loan couldn't be renewed with role %s, it could be renewed only with role" +
          " LENDER", role));
    }
  }

  @Override
  public void updateTransactionDetails(String dcbTransactionId, DcbUpdateTransaction dcbUpdateTransaction) {
    var transactionEntity = getTransactionEntityOrThrow(dcbTransactionId);
    if (!TransactionStatus.StatusEnum.CREATED.equals(transactionEntity.getStatus())) {
      throw new StatusException(String.format(
        "Transaction details should not be updated from %s status, it can be updated only from CREATED status", transactionEntity.getStatus()));
    }
    if (LENDER.equals(transactionEntity.getRole())) {
      throw new IllegalArgumentException("Item details cannot be updated for lender role");
    }
    baseLibraryService.updateTransactionDetails(transactionEntity, dcbUpdateTransaction.getItem());
  }

  private TransactionStatusResponse generateTransactionStatusResponseFromTransactionEntity(
    TransactionEntity transactionEntity, Optional<LoanRenewalDetails> loanRenewalDetails) {
    return TransactionStatusResponse.builder()
      .status(TransactionStatusResponse.StatusEnum.fromValue(transactionEntity.getStatus().getValue()))
      .item(getDcbItemForTransactionStatus(transactionEntity, loanRenewalDetails).orElse(null))
      .role((TransactionStatusResponse.RoleEnum.fromValue(transactionEntity.getRole().getValue())))
      .build();
  }

  public TransactionEntity getTransactionEntityOrThrow(String dcbTransactionId) {
    return transactionRepository.findById(dcbTransactionId)
      .orElseThrow(() -> new NotFoundException(String.format("DCB Transaction was not found by id= %s ", dcbTransactionId)));
  }

  private void checkTransactionExistsAndThrow(String dcbTransactionId) {
    if (transactionRepository.existsById(dcbTransactionId)) {
      throw new ResourceAlreadyExistException(
        String.format("unable to create transaction with id %s as it already exists", dcbTransactionId));
    }
  }

  private Optional<DcbItem> getDcbItemForTransactionStatus(
    TransactionEntity tx, Optional<LoanRenewalDetails> loanRenewalDetails) {
    var renewalInfo = loanRenewalDetails.map(this::createRenewalInfo).orElse(null);
    var holdCount = getOpenHoldRequestsNumber(tx).orElse(null);
    if (ObjectUtils.allNull(renewalInfo, holdCount)) {
      return Optional.empty();
    }

    return Optional.of(DcbItem.builder()
      .renewalInfo(renewalInfo)
      .holdCount(holdCount)
      .build());
  }

  private Optional<Integer> getOpenHoldRequestsNumber(TransactionEntity transactionEntity) {
    if (transactionEntity.getRole() != LENDER) {
      return Optional.empty();
    }

    var cqlQuery = CqlQuery.createForOpenHoldRequests(transactionEntity.getItemId());
    var circulationRequests = circulationClient.findByQuery(cqlQuery, 0);
    return Optional.ofNullable(circulationRequests).map(ResultList::getTotalRecords);
  }

  private  RenewalInfo createRenewalInfo(LoanRenewalDetails loanDetails) {
    return RenewalInfo.builder()
      .renewalCount(loanDetails.loanRenewalCount())
      .renewalMaxCount(loanDetails.renewalMaxCount())
      .renewable(loanDetails.renewable())
      .build();
  }

  private void setRenewalNumberForVirtualLoan(TransactionEntity entity, int renewalCount) {
    var virtualItemLoansResult = circulationClient.fetchLoanByQuery(buildLoanQuery(entity));
    var virtualItemLoans = virtualItemLoansResult.getLoans();
    if (CollectionUtils.isEmpty(virtualItemLoans)) {
      throw new StatusException("Virtual loan for DCB transaction not found.");
    }

    if (virtualItemLoans.size() > 1) {
      var id = entity.getId();
      log.debug("Multiple virtual loans found for DCB transaction id: {}", id);
      throw new StatusException("Multiple virtual loans found for DCB transaction.");
    }

    var virtualItemLoan = virtualItemLoans.getFirst();
    virtualItemLoan.setRenewalCount(Integer.toString(renewalCount));
    circulationClient.updateLoan(virtualItemLoan.getId(), virtualItemLoan);
  }
}
